package co.tz.tfc.ifmis.salerequest;

import co.tz.tfc.ifmis.audit.AuditService;
import co.tz.tfc.ifmis.catalog.Product;
import co.tz.tfc.ifmis.catalog.ProductRepository;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.common.exception.NotFoundException;
import co.tz.tfc.ifmis.esb.TfraClient;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import co.tz.tfc.ifmis.farmer.EntitlementService;
import co.tz.tfc.ifmis.salerequest.dto.CreateSaleRequestRequest;
import co.tz.tfc.ifmis.staff.StaffAccess;
import co.tz.tfc.ifmis.staff.StaffUser;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prepares a subsidised sale (reserve stock, compute amounts) and later hands off to a warehouse
 * staff member to finish it for real against TFRA, in the order TFRA expects:
 * {@link #submitToTfra} (bag QR codes) → {@link #requestFarmerOtp} → {@link #complete} (farmer's OTP).
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
    private final EntitlementService entitlementService;
    private final StaffAccess staffAccess;
    private final AuditService auditService;

    @Transactional
    public SaleRequest create(Long farmerId, CreateSaleRequestRequest request) {
        // Locked so two concurrent requests from the same farmer can't both fit in the same remaining entitlement.
        PortalUser farmer = portalUserRepository.findForUpdate(farmerId)
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

        // Remaining = entitled - already received (TFRA) - held by this farmer's other open reservations.
        Map<String, EntitlementService.Line> remaining = entitlementService.summarise(farmer.getId(), details);
        Map<String, Integer> requestedKgByType = new HashMap<>();

        SaleRequest saleRequest = new SaleRequest();
        saleRequest.setReference("SR-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        saleRequest.setFarmer(farmer);
        saleRequest.setWarehouse(warehouse);
        saleRequest.setExpiresAt(Instant.now().plus(RESERVATION_HOURS, ChronoUnit.HOURS));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal farmerCashAmount = BigDecimal.ZERO;

        // Lock products in a fixed order so two reservations touching the same products can't deadlock.
        List<CreateSaleRequestRequest.Line> lines = request.items().stream()
                .sorted(Comparator.comparing(CreateSaleRequestRequest.Line::productId))
                .toList();
        if (lines.stream().map(CreateSaleRequestRequest.Line::productId).distinct().count() != lines.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Each product can only appear once in a request.");
        }

        for (CreateSaleRequestRequest.Line line : lines) {
            Product product = productRepository.findForUpdate(line.productId())
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

            String type = product.getType().name();
            int requestedKg = requestedKgByType.merge(type, product.getBagSizeKg() * line.bagCount(), Integer::sum);
            EntitlementService.Line entitlement = remaining.get(type);
            if (entitlement == null || requestedKg > entitlement.remainingKg()) {
                int left = entitlement == null ? 0 : entitlement.remainingKg();
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "This request exceeds your remaining " + type + " entitlement (" + left + " kg left).");
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
        auditService.record("SALE_REQUEST_CREATED", "SaleRequest", saleRequest.getId(), warehouse.getId());
        return saleRequest;
    }

    @Transactional
    public SaleRequest cancel(Long farmerId, String reference) {
        SaleRequest saleRequest = requireOwnedByFarmer(farmerId, reference);
        if (!saleRequest.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "This request can no longer be cancelled.");
        }
        if (saleRequest.isSubmittedToTfra()) {
            throw new ApiException(HttpStatus.CONFLICT, "This sale is already being completed at the warehouse and can't be cancelled online.");
        }
        restock(saleRequest);
        saleRequest.setStatus(SaleRequestStatus.CANCELLED);
        saleRequest.setCancelledAt(Instant.now());
        auditService.record("SALE_REQUEST_CANCELLED", "SaleRequest", saleRequest.getId(), saleRequest.getWarehouse().getId());
        return saleRequest;
    }

    @Transactional(readOnly = true)
    public SaleRequest findForStaff(StaffUser staff, String reference) {
        SaleRequest saleRequest = saleRequestRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Sale request not found."));
        staffAccess.requireWarehouse(staff, saleRequest.getWarehouse());
        return saleRequest;
    }

    /** Staff opens the request at the counter once the farmer has arrived and paid the cash part. */
    @Transactional
    public SaleRequest startPickup(StaffUser staff, String reference) {
        SaleRequest saleRequest = requireOpenForStaff(staff, reference);
        if (saleRequest.getStatus() != SaleRequestStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "This request is not awaiting pickup.");
        }
        saleRequest.setStatus(SaleRequestStatus.READY_FOR_PICKUP);
        auditService.record("SALE_REQUEST_READY_FOR_PICKUP", "SaleRequest", saleRequest.getId(), saleRequest.getWarehouse().getId());
        return saleRequest;
    }

    /** Counter step 1: submit the sale to TFRA with the QR code of every bag being handed over. */
    @Transactional
    public SaleRequest submitToTfra(StaffUser staff, String reference, List<String> bagQrCodes) {
        SaleRequest saleRequest = requireOpenForStaff(staff, reference);
        if (saleRequest.getStatus() != SaleRequestStatus.READY_FOR_PICKUP) {
            throw new ApiException(HttpStatus.CONFLICT, "Mark the request ready for pickup first.");
        }
        if (saleRequest.isSubmittedToTfra()) {
            throw new ApiException(HttpStatus.CONFLICT, "This sale was already submitted to TFRA — request the farmer's OTP next.");
        }
        List<String> codes = bagQrCodes.stream().map(String::trim).filter(c -> !c.isEmpty()).toList();
        if (new HashSet<>(codes).size() != codes.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The same bag QR code was scanned more than once.");
        }
        if (codes.size() != saleRequest.totalBags()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Scan exactly one QR code per bag: expected " + saleRequest.totalBags() + ", got " + codes.size() + ".");
        }

        String tfraPendingSaleId = tfraClient.submitSale(saleRequest.getWarehouse().getTfraWarehouseId(),
                        saleRequest.getFarmer().getTfraNumber(), saleRequest.getReference(), codes)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "TFRA rejected these bag QR codes."));

        saleRequest.getBagQrCodes().clear();
        saleRequest.getBagQrCodes().addAll(codes);
        saleRequest.setTfraPendingSaleId(tfraPendingSaleId);
        saleRequest.setTfraSubmittedAt(Instant.now());
        auditService.record("SALE_REQUEST_SUBMITTED_TO_TFRA", "SaleRequest", saleRequest.getId(), saleRequest.getWarehouse().getId());
        return saleRequest;
    }

    /** Counter step 2: TFRA texts the farmer an OTP for the pending sale. */
    @Transactional
    public String requestFarmerOtp(StaffUser staff, String reference) {
        SaleRequest saleRequest = requireOpenForStaff(staff, reference);
        if (!saleRequest.isSubmittedToTfra()) {
            throw new ApiException(HttpStatus.CONFLICT, "Submit the bag QR codes to TFRA first.");
        }
        String requestId = tfraClient.requestOtp(saleRequest.getWarehouse().getTfraWarehouseId(),
                saleRequest.getFarmer().getTfraNumber(), saleRequest.getTfraPendingSaleId());
        auditService.record("SALE_REQUEST_OTP_REQUESTED", "SaleRequest", saleRequest.getId(), saleRequest.getWarehouse().getId());
        return requestId;
    }

    /** Counter step 3: confirm with the OTP the farmer read from their SMS — finalises the sale. */
    @Transactional
    public SaleRequest complete(StaffUser staff, String reference, String otp) {
        SaleRequest saleRequest = requireOpenForStaff(staff, reference);
        if (!saleRequest.isSubmittedToTfra()) {
            throw new ApiException(HttpStatus.CONFLICT, "Submit the bag QR codes to TFRA first.");
        }
        String tfraReference = tfraClient.confirmSale(saleRequest.getWarehouse().getTfraWarehouseId(), saleRequest.getTfraPendingSaleId(), otp)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "The farmer's OTP is incorrect or has expired."));

        saleRequest.setStatus(SaleRequestStatus.COMPLETED);
        saleRequest.setCompletedAt(Instant.now());
        saleRequest.setCompletedBy(staff.getId());
        saleRequest.setTfraSaleReference(tfraReference);
        auditService.record("SALE_REQUEST_COMPLETED", "SaleRequest", saleRequest.getId(), saleRequest.getWarehouse().getId());
        return saleRequest;
    }

    /**
     * Releases stock held by any reservation whose 48-hour window has passed. Run periodically.
     * Requests already submitted to TFRA are left for staff to finish — their bags are at the counter.
     */
    @Scheduled(fixedRate = 15, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    @Transactional
    public void expireStaleReservations() {
        List<SaleRequest> stale = saleRequestRepository.findByStatusInAndExpiresAtBefore(
                List.of(SaleRequestStatus.SUBMITTED, SaleRequestStatus.READY_FOR_PICKUP), Instant.now()).stream()
                .filter(r -> !r.isSubmittedToTfra())
                .toList();
        for (SaleRequest saleRequest : stale) {
            restock(saleRequest);
            saleRequest.setStatus(SaleRequestStatus.EXPIRED);
            auditService.record("SALE_REQUEST_EXPIRED", "SaleRequest", saleRequest.getId(), saleRequest.getWarehouse().getId());
        }
        if (!stale.isEmpty()) {
            saleRequestRepository.saveAll(stale);
        }
    }

    private void restock(SaleRequest saleRequest) {
        for (SaleRequestItem item : saleRequest.getItems()) {
            Product product = productRepository.findForUpdate(item.getProduct().getId()).orElseThrow();
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

    private SaleRequest requireOpenForStaff(StaffUser staff, String reference) {
        SaleRequest saleRequest = findForStaff(staff, reference);
        if (!saleRequest.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "This sale request is not open.");
        }
        // Once submitted to TFRA the bags are physically at the counter, so let staff finish even past expiry.
        if (saleRequest.isExpired() && !saleRequest.isSubmittedToTfra()) {
            throw new ApiException(HttpStatus.CONFLICT, "This reservation has expired.");
        }
        return saleRequest;
    }
}
