package co.tz.tfc.ifmis.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyAuthRequest(
        @NotBlank String tfraNumber,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code
) {
}
