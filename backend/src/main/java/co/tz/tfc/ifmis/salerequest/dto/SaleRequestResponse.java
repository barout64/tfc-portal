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
        String warehouseName,
        BigDecimal totalAmount,
        BigDecimal farmerCashAmount,
        BigDecimal subsidyAmount,
        Instant reservedAt,
        Instant expiresAt,
        Instant completedAt,
        List<Line> items
) {
    public record Line(String productName, int bagCount, BigDecimal fullUnitPrice, BigDecimal farmerUnitPrice) {
    }

    public static SaleRequestResponse of(SaleRequest r) {
        return new SaleRequestResponse(
                r.getId(),
                r.getReference(),
                r.getStatus(),
                r.getWarehouse().getName(),
                r.getTotalAmount(),
                r.getFarmerCashAmount(),
                r.getSubsidyAmount(),
                r.getReservedAt(),
                r.getExpiresAt(),
                r.getCompletedAt(),
                r.getItems().stream()
                        .map(i -> new Line(i.getProduct().getName(), i.getBagCount(), i.getFullUnitPrice(), i.getFarmerUnitPrice()))
                        .toList()
        );
    }
}
