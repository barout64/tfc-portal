package co.tz.tfc.ifmis.salerequest;

import co.tz.tfc.ifmis.audit.AuditService;
import co.tz.tfc.ifmis.catalog.Product;
import co.tz.tfc.ifmis.catalog.ProductRepository;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.common.exception.NotFoundException;
import co.tz.tfc.ifmis.esb.TfraClient;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import co.tz.tfc.ifmis.salerequest.dto.CreateSaleRequestRequest;
import co.tz.tfc.ifmis.user.PortalUser;
import co.tz.tfc.ifmis.user.PortalUserRepository;
import co.tz.tfc.ifmis.warehouse.Warehouse;
import co.tz.tfc.ifmis.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prepares a subsidised sale (reserve stock, compute amounts) and later hands off to a warehouse
 * staff member to finish it for real against TFRA (QR scan + OTP) — see {@link #complete}.
 */
@Service
@RequiredArgsConstructor
public class SaleRequestService {

    private static final int RESERVATION_HOURS = 48;

    private final SaleRequestRepository saleRequestRepository;
    private final PortalUserRepository portalUserRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final TfraClient tfraClient;
    private final AuditService auditService;

    @Transactional
    public SaleRequest create(Long farmerId, CreateSaleRequestRequest request) {
        PortalUser farmer = portalUserRepository.findById(farmerId)
                .orElseThrow(() -> new NotFoundException("Farmer account not found."));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found."));
        if (!warehouse.canTakeSubsidisedSales()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This warehouse cannot take subsidised sales right now — it is not linked with TFRA.");
        }

        // Re-check live entitlement with TFRA rather than trusting anything cached client-side.
        TfraFarmerDetails details = tfraClient.getFarmerDetails(farmer.getTfraNumber())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Could not re-verify this farmer with TFRA."));
        if (!details.active() || details.stopSubsidy()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This farmer is not currently eligible for the subsidy programme.");
        }

        // Entitlement is checked against what TFRA reports for this request alone; TFRA is the
        // system of record for how much of it earlier requests already consumed.
        Map<String, Integer> requestedKgByType = new HashMap<>();

        SaleRequest saleRequest = new SaleRequest();
        saleRequest.setReference("SR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        saleRequest.setFarmer(farmer);
        saleRequest.setWarehouse(warehouse);
        saleRequest.setExpiresAt(Instant.now().plus(RESERVATION_HOURS, ChronoUnit.HOURS));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal farmerCashAmount = BigDecimal.ZERO;

        for (CreateSaleRequestRequest.Line line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found."));
            if (!product.isActive() || !product.getWarehouse().getId().equals(warehouse.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Product " + product.getName() + " is not available at this warehouse.");
            }
            if (!product.isSubsidised()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, product.getName() + " is not part of the subsidy programme.");
            }
            if (product.getInternalQuantity() < line.bagCount()) {
                throw new ApiException(HttpStatus.CONFLICT, product.getName() + " does not have enough stock for this request.");
            }

            int requestedKg = product.getBagSizeKg() * line.bagCount();
            requestedKgByType.merge(product.getType().name(), requestedKg, Integer::sum);
            Integer entitlementKg = details.entitlementByType().get(product.getType().name());
            if (entitlementKg == null || requestedKgByType.get(product.getType().name()) > entitlementKg) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This request exceeds the farmer's remaining " + product.getType() + " entitlement.");
            }

            product.setInternalQuantity(product.getInternalQuantity() - line.bagCount());
            productRepository.save(product);

            SaleRequestItem item = new SaleRequestItem();
            item.setSaleRequest(saleRequest);
            item.setProduct(product);
            item.setBagCount(line.bagCount());
            item.setFullUnitPrice(product.getFullPrice());
            item.setFarmerUnitPrice(product.getFarmerUnitPrice());
            saleRequest.getItems().add(item);

            totalAmount = totalAmount.add(product.getFullPrice().multiply(BigDecimal.valueOf(line.bagCount())));
            farmerCashAmount = farmerCashAmount.add(product.getFarmerUnitPrice().multiply(BigDecimal.valueOf(line.bagCount())));
        }

        saleRequest.setTotalAmount(totalAmount);
        saleRequest.setFarmerCashAmount(farmerCashAmount);
        saleRequest.setSubsidyAmount(totalAmount.subtract(farmerCashAmount));

        saleRequest = saleRequestRepository.save(saleRequest);
        auditService.record("SALE_REQUEST_CREATED", "SaleRequest", saleRequest.getId());
        return saleRequest;
    }

    @Transactional
    public SaleRequest cancel(Long farmerId, String reference) {
        SaleRequest saleRequest = requireOwnedByFarmer(farmerId, reference);
        if (!saleRequest.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "This request can no longer be cancelled.");
        }
        restock(saleRequest);
        saleRequest.setStatus(SaleRequestStatus.CANCELLED);
        saleRequest.setCancelledAt(Instant.now());
        auditService.record("SALE_REQUEST_CANCELLED", "SaleRequest", saleRequest.getId());
        return saleRequest;
    }

    @Transactional
    public SaleRequest startPickup(String reference) {
        SaleRequest saleRequest = requireOpenByReference(reference);
        if (saleRequest.getStatus() != SaleRequestStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "This request is not awaiting pickup.");
        }
        saleRequest.setStatus(SaleRequestStatus.READY_FOR_PICKUP);
        auditService.record("SALE_REQUEST_READY_FOR_PICKUP", "SaleRequest", saleRequest.getId());
        return saleRequest;
    }

    /** Staff-side finish: scan the physical bag QR codes, then confirm with the farmer's OTP. */
    @Transactional
    public SaleRequest complete(String reference, List<String> bagQrCodes, String otp, Long staffId) {
        SaleRequest saleRequest = requireOpenByReference(reference);

        if (!tfraClient.scanBagQrCodes(reference, bagQrCodes)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The bag QR codes could not be validated with TFRA.");
        }
        String tfraReference = tfraClient.confirmSale(reference, otp)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "The farmer's OTP is incorrect or has expired."));

        saleRequest.setStatus(SaleRequestStatus.COMPLETED);
        saleRequest.setCompletedAt(Instant.now());
        saleRequest.setCompletedBy(staffId);
        saleRequest.setTfraSaleReference(tfraReference);
        auditService.record("SALE_REQUEST_COMPLETED", "SaleRequest", saleRequest.getId());
        return saleRequest;
    }

    /** Releases stock held by any reservation whose 48-hour window has passed. Run periodically. */
    @Scheduled(fixedRate = 15, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    @Transactional
    public void expireStaleReservations() {
        List<SaleRequest> stale = saleRequestRepository.findByStatusInAndExpiresAtBefore(
                List.of(SaleRequestStatus.SUBMITTED, SaleRequestStatus.READY_FOR_PICKUP), Instant.now());
        for (SaleRequest saleRequest : stale) {
            restock(saleRequest);
            saleRequest.setStatus(SaleRequestStatus.EXPIRED);
            auditService.record("SALE_REQUEST_EXPIRED", "SaleRequest", saleRequest.getId());
        }
        if (!stale.isEmpty()) {
            saleRequestRepository.saveAll(stale);
        }
    }

    private void restock(SaleRequest saleRequest) {
        for (SaleRequestItem item : saleRequest.getItems()) {
            Product product = item.getProduct();
            product.setInternalQuantity(product.getInternalQuantity() + item.getBagCount());
            productRepository.save(product);
        }
    }

    private SaleRequest requireOwnedByFarmer(Long farmerId, String reference) {
        SaleRequest saleRequest = saleRequestRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Sale request not found."));
        if (!saleRequest.getFarmer().getId().equals(farmerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This sale request does not belong to you.");
        }
        return saleRequest;
    }

    private SaleRequest requireOpenByReference(String reference) {
        SaleRequest saleRequest = saleRequestRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Sale request not found."));
        if (!saleRequest.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "This sale request is not open.");
        }
        if (saleRequest.isExpired()) {
            throw new ApiException(HttpStatus.CONFLICT, "This reservation has expired.");
        }
        return saleRequest;
    }
}
