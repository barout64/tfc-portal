package co.tz.tfc.ifmis.salerequest;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.salerequest.dto.CompleteSaleRequestRequest;
import co.tz.tfc.ifmis.salerequest.dto.SaleRequestResponse;
import co.tz.tfc.ifmis.salerequest.dto.SubmitSaleRequestRequest;
import co.tz.tfc.ifmis.staff.StaffAccess;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Warehouse-staff side of finishing a reserved sale request — requires a STAFF JWT, and staff only
 * see requests for their own warehouse. Order at the counter: start → submit → request-otp → complete.
 */
@RestController
@RequestMapping("/api/staff/sale-requests")
@RequiredArgsConstructor
public class StaffSaleRequestController {

    private final SaleRequestService saleRequestService;
    private final StaffAccess staffAccess;

    @GetMapping("/{reference}")
    public SaleRequestResponse find(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        return SaleRequestResponse.of(saleRequestService.findForStaff(staffAccess.current(principal), reference));
    }

    @PostMapping("/{reference}/start")
    public SaleRequestResponse start(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        return SaleRequestResponse.of(saleRequestService.startPickup(staffAccess.current(principal), reference));
    }

    @PostMapping("/{reference}/submit")
    public SaleRequestResponse submit(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                        @PathVariable String reference,
                                        @Valid @RequestBody SubmitSaleRequestRequest request) {
        return SaleRequestResponse.of(saleRequestService.submitToTfra(staffAccess.current(principal), reference, request.bagQrCodes()));
    }

    @PostMapping("/{reference}/request-otp")
    public Map<String, String> requestOtp(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable String reference) {
        String requestId = saleRequestService.requestFarmerOtp(staffAccess.current(principal), reference);
        return Map.of("message", "TFRA has sent an OTP to the farmer's phone.", "requestId", requestId);
    }

    @PostMapping("/{reference}/complete")
    public SaleRequestResponse complete(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                          @PathVariable String reference,
                                          @Valid @RequestBody CompleteSaleRequestRequest request) {
        return SaleRequestResponse.of(saleRequestService.complete(staffAccess.current(principal), reference, request.otp()));
    }
}
