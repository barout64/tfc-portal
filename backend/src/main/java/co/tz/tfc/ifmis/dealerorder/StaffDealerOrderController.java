package co.tz.tfc.ifmis.dealerorder;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.dealerorder.dto.DealerOrderResponse;
import co.tz.tfc.ifmis.dealerorder.dto.RespondDealerOrderRequest;
import co.tz.tfc.ifmis.staff.StaffAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Warehouse staff handle agro-dealer inquiries and orders for their own warehouse. Requires a STAFF JWT. */
@RestController
@RequestMapping("/api/staff/dealer-orders")
@RequiredArgsConstructor
public class StaffDealerOrderController {

    private final DealerOrderService dealerOrderService;
    private final StaffAccess staffAccess;

    @GetMapping
    public List<DealerOrderResponse> list(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return dealerOrderService.listForStaff(staffAccess.current(principal)).stream()
                .map(DealerOrderResponse::of)
                .toList();
    }

    @PostMapping("/{reference}/answer")
    public DealerOrderResponse answer(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference,
                                        @Valid @RequestBody RespondDealerOrderRequest request) {
        return DealerOrderResponse.of(dealerOrderService.answer(staffAccess.current(principal), reference, request.response()));
    }

    @PostMapping("/{reference}/accept")
    public DealerOrderResponse accept(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference,
                                        @Valid @RequestBody RespondDealerOrderRequest request) {
        return DealerOrderResponse.of(dealerOrderService.accept(staffAccess.current(principal), reference, request.response()));
    }

    @PostMapping("/{reference}/reject")
    public DealerOrderResponse reject(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference,
                                        @Valid @RequestBody RespondDealerOrderRequest request) {
        return DealerOrderResponse.of(dealerOrderService.reject(staffAccess.current(principal), reference, request.response()));
    }

    @PostMapping("/{reference}/fulfil")
    public DealerOrderResponse fulfil(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        return DealerOrderResponse.of(dealerOrderService.fulfil(staffAccess.current(principal), reference));
    }
}
