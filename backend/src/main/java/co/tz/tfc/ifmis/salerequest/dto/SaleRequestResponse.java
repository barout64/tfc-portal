package co.tz.tfc.ifmis.salerequest.dto;

import co.tz.tfc.ifmis.salerequest.SaleRequest;
import co.tz.tfc.ifmis.salerequest.SaleRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleRequestResponse(
        Long id,
        String reference,
        SaleRequestStatus status,
        String farmerName,
        String farmerNumber,
        String warehouseName,
        String warehouseLocation,
        BigDecimal totalAmount,
        BigDecimal farmerCashAmount,
        BigDecimal subsidyAmount,
        Instant reservedAt,
        Instant expiresAt,
        boolean submittedToTfra,
        Instant completedAt,
        String tfraSaleReference,
        int totalBags,
        List<Line> items
) {
    public record Line(String productName, int bagSizeKg, int bagCount, BigDecimal fullUnitPrice, BigDecimal farmerUnitPrice) {
    }

    public static SaleRequestResponse of(SaleRequest r) {
        var w = r.getWarehouse();
        String location = String.join(", ", java.util.stream.Stream.of(w.getVillage(), w.getDistrict(), w.getRegion())
                .filter(part -> part != null && !part.isBlank())
                .toList());
        return new SaleRequestResponse(
                r.getId(),
                r.getReference(),
                r.getStatus(),
                r.getFarmer().getDisplayName(),
                r.getFarmer().getTfraNumber(),
                w.getName(),
                location,
                r.getTotalAmount(),
                r.getFarmerCashAmount(),
                r.getSubsidyAmount(),
                r.getReservedAt(),
                r.getExpiresAt(),
                r.isSubmittedToTfra(),
                r.getCompletedAt(),
                r.getTfraSaleReference(),
                r.totalBags(),
                r.getItems().stream()
                        .map(i -> new Line(i.getProduct().getName(), i.getProduct().getBagSizeKg(), i.getBagCount(), i.getFullUnitPrice(), i.getFarmerUnitPrice()))
                        .toList()
        );
    }
}
