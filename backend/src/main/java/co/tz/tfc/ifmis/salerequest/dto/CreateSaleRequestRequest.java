package co.tz.tfc.ifmis.salerequest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSaleRequestRequest(
        @NotNull Long warehouseId,
        @NotEmpty List<@Valid Line> items
) {
    public record Line(@NotNull Long productId, @Min(1) int bagCount) {
    }
}
