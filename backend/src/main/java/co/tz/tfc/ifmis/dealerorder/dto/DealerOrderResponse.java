package co.tz.tfc.ifmis.dealerorder.dto;

import co.tz.tfc.ifmis.dealerorder.DealerOrder;
import co.tz.tfc.ifmis.dealerorder.DealerOrderKind;
import co.tz.tfc.ifmis.dealerorder.DealerOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DealerOrderResponse(
        Long id,
        String reference,
        DealerOrderKind kind,
        DealerOrderStatus status,
        String dealerName,
        String dealerTfraNumber,
        String warehouseName,
        BigDecimal estimatedAmount,
        String dealerNote,
        String warehouseResponse,
        Instant createdAt,
        Instant respondedAt,
        List<Line> items
) {
    public record Line(String productName, int bagSizeKg, int bagCount, BigDecimal unitPrice) {
    }

    public static DealerOrderResponse of(DealerOrder o) {
        return new DealerOrderResponse(
                o.getId(),
                o.getReference(),
                o.getKind(),
                o.getStatus(),
                o.getDealer().getDisplayName(),
                o.getDealer().getTfraNumber(),
                o.getWarehouse().getName(),
                o.getEstimatedAmount(),
                o.getDealerNote(),
                o.getWarehouseResponse(),
                o.getCreatedAt(),
                o.getRespondedAt(),
                o.getItems().stream()
                        .map(i -> new Line(i.getProduct().getName(), i.getProduct().getBagSizeKg(), i.getBagCount(), i.getUnitPrice()))
                        .toList()
        );
    }
}
