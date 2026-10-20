package co.tz.tfc.ifmis.auth;

import co.tz.tfc.ifmis.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = properties.getExpiryMinutes();
    }

    public String issue(String subjectType, Long id, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subjectType + ":" + id)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiryMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Optional<AuthenticatedPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String[] subject = claims.getSubject().split(":", 2);
            return Optional.of(new AuthenticatedPrincipal(subject[0], Long.valueOf(subject[1]), claims.get("role", String.class)));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
