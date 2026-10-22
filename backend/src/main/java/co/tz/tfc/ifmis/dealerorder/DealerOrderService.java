package co.tz.tfc.ifmis.dealerorder;

import co.tz.tfc.ifmis.audit.AuditService;
import co.tz.tfc.ifmis.catalog.Product;
import co.tz.tfc.ifmis.catalog.ProductRepository;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.common.exception.NotFoundException;
import co.tz.tfc.ifmis.dealerorder.dto.CreateDealerOrderRequest;
import co.tz.tfc.ifmis.staff.StaffAccess;
import co.tz.tfc.ifmis.staff.StaffRole;
import co.tz.tfc.ifmis.staff.StaffUser;
import co.tz.tfc.ifmis.user.PortalUser;
import co.tz.tfc.ifmis.user.PortalUserRepository;
import co.tz.tfc.ifmis.warehouse.Warehouse;
import co.tz.tfc.ifmis.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Wholesale (non-subsidised) inquiries and orders from agro-dealers — the normal TFC sale flow, so no
 * TFRA calls and no subsidy. Stock is only taken when the warehouse accepts an order, not on submit.
 */
@Service
@RequiredArgsConstructor
public class DealerOrderService {

    private final DealerOrderRepository dealerOrderRepository;
    private final PortalUserRepository portalUserRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StaffAccess staffAccess;
    private final AuditService auditService;

    @Transactional
    public DealerOrder create(Long dealerId, CreateDealerOrderRequest request) {
        PortalUser dealer = portalUserRepository.findById(dealerId)
                .orElseThrow(() -> new NotFoundException("Agro-dealer account not found."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .filter(Warehouse::isActive)
                .orElseThrow(() -> new NotFoundException("Warehouse not found."));
        if (request.items().stream().map(CreateDealerOrderRequest.Line::productId).distinct().count() != request.items().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Each product can only appear once in a request.");
        }

        DealerOrder order = new DealerOrder();
        order.setReference((request.kind() == DealerOrderKind.ORDER ? "DO-" : "DI-") + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        order.setDealer(dealer);
        order.setWarehouse(warehouse);
        order.setKind(request.kind());
        order.setDealerNote(blankToNull(request.note()));

        BigDecimal estimate = BigDecimal.ZERO;
        for (CreateDealerOrderRequest.Line line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product not found."));
            if (!product.isActive() || !product.getWarehouse().getId().equals(warehouse.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Product " + product.getName() + " is not available at this warehouse.");
            }
            DealerOrderItem item = new DealerOrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setBagCount(line.bagCount());
            item.setUnitPrice(product.getFullPrice());
            order.getItems().add(item);
            estimate = estimate.add(product.getFullPrice().multiply(BigDecimal.valueOf(line.bagCount())));
        }
        order.setEstimatedAmount(estimate);

        order = dealerOrderRepository.save(order);
        auditService.record("DEALER_" + request.kind() + "_CREATED", "DealerOrder", order.getId(), warehouse.getId());
        return order;
    }

    @Transactional
    public DealerOrder cancel(Long dealerId, String reference) {
        DealerOrder order = dealerOrderRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Order not found."));
        if (!order.getDealer().getId().equals(dealerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This order does not belong to you.");
        }
        if (order.getStatus() != DealerOrderStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "The warehouse has already responded — contact them to change it.");
        }
        order.setStatus(DealerOrderStatus.CANCELLED);
        auditService.record("DEALER_ORDER_CANCELLED", "DealerOrder", order.getId(), order.getWarehouse().getId());
        return order;
    }

    @Transactional(readOnly = true)
    public List<DealerOrder> listForStaff(StaffUser staff) {
        return staff.getRole() == StaffRole.ADMIN
                ? dealerOrderRepository.findAllByOrderByCreatedAtDesc()
                : dealerOrderRepository.findByWarehouse_IdOrderByCreatedAtDesc(staff.getWarehouse().getId());
    }

    @Transactional
    public DealerOrder answer(StaffUser staff, String reference, String response) {
        DealerOrder order = requireSubmitted(staff, reference, DealerOrderKind.INQUIRY);
        if (response == null || response.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Write an answer for the agro-dealer.");
        }
        return respond(staff, order, DealerOrderStatus.ANSWERED, response);
    }

    @Transactional
    public DealerOrder accept(StaffUser staff, String reference, String response) {
        DealerOrder order = requireSubmitted(staff, reference, DealerOrderKind.ORDER);
        List<DealerOrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();
        for (DealerOrderItem item : items) {
            Product product = productRepository.findForUpdate(item.getProduct().getId()).orElseThrow();
            if (product.getInternalQuantity() < item.getBagCount()) {
                throw new ApiException(HttpStatus.CONFLICT, "Not enough " + product.getName() + " in stock to accept this order.");
            }
            product.setInternalQuantity(product.getInternalQuantity() - item.getBagCount());
            productRepository.save(product);
        }
        return respond(staff, order, DealerOrderStatus.ACCEPTED, response);
    }

    @Transactional
    public DealerOrder reject(StaffUser staff, String reference, String response) {
        DealerOrder order = requireSubmitted(staff, reference, DealerOrderKind.ORDER);
        return respond(staff, order, DealerOrderStatus.REJECTED, response);
    }

    @Transactional
    public DealerOrder fulfil(StaffUser staff, String reference) {
        DealerOrder order = requireForStaff(staff, reference);
        if (order.getStatus() != DealerOrderStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an accepted order can be marked fulfilled.");
        }
        order.setStatus(DealerOrderStatus.FULFILLED);
        auditService.record("DEALER_ORDER_FULFILLED", "DealerOrder", order.getId(), order.getWarehouse().getId());
        return order;
    }

    private DealerOrder respond(StaffUser staff, DealerOrder order, DealerOrderStatus status, String response) {
        order.setStatus(status);
        order.setWarehouseResponse(blankToNull(response));
        order.setRespondedAt(Instant.now());
        order.setRespondedBy(staff.getId());
        auditService.record("DEALER_ORDER_" + status, "DealerOrder", order.getId(), order.getWarehouse().getId());
        return order;
    }

    private DealerOrder requireSubmitted(StaffUser staff, String reference, DealerOrderKind kind) {
        DealerOrder order = requireForStaff(staff, reference);
        if (order.getKind() != kind) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That action doesn't apply to an " + order.getKind().name().toLowerCase() + ".");
        }
        if (order.getStatus() != DealerOrderStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "This has already been handled.");
        }
        return order;
    }

    private DealerOrder requireForStaff(StaffUser staff, String reference) {
        DealerOrder order = dealerOrderRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Order not found."));
        staffAccess.requireWarehouse(staff, order.getWarehouse());
        return order;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
