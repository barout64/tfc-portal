package co.tz.tfc.ifmis.salerequest;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.common.exception.NotFoundException;
import co.tz.tfc.ifmis.esb.TfraClient;
import co.tz.tfc.ifmis.salerequest.dto.CompleteSaleRequestRequest;
import co.tz.tfc.ifmis.salerequest.dto.SaleRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Warehouse-staff side of finishing a reserved sale request — requires a STAFF JWT. */
@RestController
@RequestMapping("/api/staff/sale-requests")
@RequiredArgsConstructor
public class StaffSaleRequestController {

    private final SaleRequestService saleRequestService;
    private final SaleRequestRepository saleRequestRepository;
    private final TfraClient tfraClient;

    @GetMapping("/{reference}")
    public SaleRequestResponse find(@PathVariable String reference) {
        return SaleRequestResponse.of(saleRequestRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Sale request not found.")));
    }

    @PostMapping("/{reference}/start")
    public SaleRequestResponse start(@PathVariable String reference) {
        return SaleRequestResponse.of(saleRequestService.startPickup(reference));
    }

    @PostMapping("/{reference}/request-otp")
    public Map<String, String> requestOtp(@PathVariable String reference) {
        SaleRequest saleRequest = saleRequestRepository.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Sale request not found."));
        String requestId = tfraClient.requestOtp(saleRequest.getFarmer().getTfraNumber());
        return Map.of("message", "An OTP has been requested from TFRA for the farmer's phone.", "requestId", requestId);
    }

    @PostMapping("/{reference}/complete")
    public SaleRequestResponse complete(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                          @PathVariable String reference,
                                          @Valid @RequestBody CompleteSaleRequestRequest request) {
        return SaleRequestResponse.of(saleRequestService.complete(reference, request.bagQrCodes(), request.otp(), principal.id()));
    }
}
