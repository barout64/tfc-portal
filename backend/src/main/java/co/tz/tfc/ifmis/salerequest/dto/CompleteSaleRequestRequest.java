package co.tz.tfc.ifmis.salerequest.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteSaleRequestRequest(@NotBlank String otp) {
}
