package co.tz.tfc.ifmis.farmer;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.common.exception.NotFoundException;
import co.tz.tfc.ifmis.esb.TfraClient;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import co.tz.tfc.ifmis.farmer.dto.FarmerProfileResponse;
import co.tz.tfc.ifmis.user.PortalUser;
import co.tz.tfc.ifmis.user.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Farmer self-service — requires a FARMER JWT. Only ever looks up the caller's own farmer number. */
@RestController
@RequestMapping("/api/farmers/me")
@RequiredArgsConstructor
public class FarmerController {

    private final PortalUserRepository portalUserRepository;
    private final TfraClient tfraClient;
    private final EntitlementService entitlementService;

    @GetMapping
    public FarmerProfileResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        PortalUser farmer = portalUserRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Farmer account not found."));
        TfraFarmerDetails details = tfraClient.getFarmerDetails(farmer.getTfraNumber())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_GATEWAY, "Could not load your details from TFRA right now."));

        return new FarmerProfileResponse(
                details.farmerNumber(),
                details.fullName(),
                details.active(),
                details.stopSubsidy(),
                details.active() && !details.stopSubsidy(),
                entitlementService.summarise(farmer.getId(), details).values().stream().toList(),
                details.orders(),
                details.receivedItems()
        );
    }
}
