package co.tz.tfc.ifmis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tfc.jwt")
public class JwtProperties {

    /** Base64 or plain secret — set via TFC_JWT_SECRET in every real environment, never committed. */
    private String secret = "dev-only-insecure-secret-change-me-before-any-real-deployment-32bytes";

    private long expiryMinutes = 60;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(long expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
