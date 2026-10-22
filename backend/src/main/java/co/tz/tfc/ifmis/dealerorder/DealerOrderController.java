package co.tz.tfc.ifmis.dealerorder;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.dealerorder.dto.CreateDealerOrderRequest;
import co.tz.tfc.ifmis.dealerorder.dto.DealerOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Agro-dealer-facing: send and track wholesale inquiries/orders. Requires an AGRO_DEALER JWT. */
@RestController
@RequestMapping("/api/agro-dealers/me/orders")
@RequiredArgsConstructor
public class DealerOrderController {

    private final DealerOrderService dealerOrderService;
    private final DealerOrderRepository dealerOrderRepository;

    @PostMapping
    public ResponseEntity<DealerOrderResponse> create(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                        @Valid @RequestBody CreateDealerOrderRequest request) {
        DealerOrder order = dealerOrderService.create(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DealerOrderResponse.of(order));
    }

    @GetMapping
    public List<DealerOrderResponse> mine(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return dealerOrderRepository.findByDealer_IdOrderByCreatedAtDesc(principal.id()).stream()
                .map(DealerOrderResponse::of)
                .toList();
    }

    @PostMapping("/{reference}/cancel")
    public DealerOrderResponse cancel(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        return DealerOrderResponse.of(dealerOrderService.cancel(principal.id(), reference));
    }
}
