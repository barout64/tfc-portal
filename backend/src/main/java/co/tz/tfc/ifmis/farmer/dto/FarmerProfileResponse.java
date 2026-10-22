package co.tz.tfc.ifmis.farmer.dto;

import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import co.tz.tfc.ifmis.farmer.EntitlementService;

import java.util.List;

/** "My entitlement" — what TFRA knows about the logged-in farmer, plus what is left to buy. */
public record FarmerProfileResponse(
        String farmerNumber,
        String fullName,
        boolean active,
        boolean stopSubsidy,
        boolean eligible,
        List<EntitlementService.Line> entitlements,
        List<TfraFarmerDetails.Order> orders,
        List<TfraFarmerDetails.ReceivedItem> receivedItems
) {
}
