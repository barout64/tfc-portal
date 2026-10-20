package co.tz.tfc.ifmis.auth.dto;

public record TokenResponse(String token, String displayName, String role) {
}
