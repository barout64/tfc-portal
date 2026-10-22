package co.tz.tfc.ifmis.dealerorder.dto;

import co.tz.tfc.ifmis.dealerorder.DealerOrderKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateDealerOrderRequest(
        @NotNull Long warehouseId,
        @NotNull DealerOrderKind kind,
        @NotEmpty List<@Valid Line> items,
        @Size(max = 1000) String note
) {
    public record Line(@NotNull Long productId, @Min(1) int bagCount) {
    }
}
