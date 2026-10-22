package co.tz.tfc.ifmis.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 40)
    private String subjectType;

    @Column(nullable = false)
    private Long subjectId;

    /** Who triggered the action, e.g. "FARMER:42" or "STAFF:7" — null for system-initiated events. */
    @Column(length = 60)
    private String actor;

    /** Warehouse the action concerns, when there is one — so "which warehouse" is answerable from the log. */
    private Long warehouseId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
