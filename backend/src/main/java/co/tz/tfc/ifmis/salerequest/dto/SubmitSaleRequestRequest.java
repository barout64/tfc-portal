package co.tz.tfc.ifmis.salerequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitSaleRequestRequest(@NotEmpty List<@NotBlank String> bagQrCodes) {
}
