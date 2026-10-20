package co.tz.tfc.ifmis.salerequest;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.salerequest.dto.CreateSaleRequestRequest;
import co.tz.tfc.ifmis.salerequest.dto.SaleRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Farmer-facing: create, list and track their own sale requests. Requires a FARMER JWT. */
@RestController
@RequestMapping("/api/sale-requests")
@RequiredArgsConstructor
public class SaleRequestController {

    private final SaleRequestService saleRequestService;
    private final SaleRequestRepository saleRequestRepository;

    @PostMapping
    public ResponseEntity<SaleRequestResponse> create(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                        @Valid @RequestBody CreateSaleRequestRequest request) {
        SaleRequest saleRequest = saleRequestService.create(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SaleRequestResponse.of(saleRequest));
    }

    @GetMapping
    public List<SaleRequestResponse> mine(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return saleRequestRepository.findByFarmer_IdOrderByReservedAtDesc(principal.id()).stream()
                .map(SaleRequestResponse::of)
                .toList();
    }

    @GetMapping("/{reference}")
    public SaleRequestResponse track(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        SaleRequest saleRequest = saleRequestRepository.findByReference(reference)
                .orElseThrow(() -> new co.tz.tfc.ifmis.common.exception.NotFoundException("Sale request not found."));
        if (!saleRequest.getFarmer().getId().equals(principal.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This sale request does not belong to you.");
        }
        return SaleRequestResponse.of(saleRequest);
    }

    @PostMapping("/{reference}/cancel")
    public SaleRequestResponse cancel(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        return SaleRequestResponse.of(saleRequestService.cancel(principal.id(), reference));
    }
}
