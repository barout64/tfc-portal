package co.tz.tfc.ifmis.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "portal_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"type", "tfra_number"}),
        @UniqueConstraint(columnNames = {"phone"})
})
@Getter
@Setter
@NoArgsConstructor
public class PortalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PortalUserType type;

    /** Farmer wallet number (farmer) or TFRA agro-dealer number (agro-dealer) — never a login secret by itself. */
    @Column(name = "tfra_number", nullable = false, length = 40)
    private String tfraNumber;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PortalUserStatus status = PortalUserStatus.PENDING_VERIFICATION;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant verifiedAt;
}
