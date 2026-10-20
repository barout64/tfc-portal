package co.tz.tfc.ifmis.esb.dto;

import java.util.Map;

/** Mirrors the shape of {@code POST /tfra/farmers/details}. */
public record TfraFarmerDetails(
        String farmerNumber,
        String fullName,
        String phoneOnFile,
        boolean active,
        boolean stopSubsidy,
        Map<String, Integer> entitlementByType
) {
}
