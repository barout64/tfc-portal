package co.tz.tfc.ifmis.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for both the registration-start and login-start endpoints — only the TFRA number is needed. */
public record StartAuthRequest(@NotBlank String tfraNumber) {
}
