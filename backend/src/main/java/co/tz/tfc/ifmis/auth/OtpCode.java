package co.tz.tfc.ifmis.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portal_user_id", nullable = false)
    private Long portalUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    /** SHA-256 hex digest of the 6-digit code — the plaintext code only ever goes out over SMS. */
    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant consumedAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public boolean isUsable() {
        return consumedAt == null && attempts < 5 && Instant.now().isBefore(expiresAt);
    }
}
