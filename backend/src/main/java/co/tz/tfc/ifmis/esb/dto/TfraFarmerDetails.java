package co.tz.tfc.ifmis.esb.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Mirrors the shape of {@code POST /tfra/farmers/details}. Entitlement and quantities are in kg. */
public record TfraFarmerDetails(
        String farmerNumber,
        String fullName,
        String phoneOnFile,
        boolean active,
        boolean stopSubsidy,
        Map<String, Integer> entitlementByType,
        List<Order> orders,
        List<ReceivedItem> receivedItems
) {
    /** A subsidised order TFRA has on record for this farmer (from any seller, not only TFC). */
    public record Order(String orderNumber, String productType, String productName, int quantityKg, String status, LocalDate orderedOn) {
    }

    /** Inputs TFRA has confirmed this farmer already received — counts against the entitlement. */
    public record ReceivedItem(String productType, String productName, int quantityKg, String sellerName, LocalDate receivedOn) {
    }
}
