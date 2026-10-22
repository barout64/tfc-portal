package co.tz.tfc.ifmis.dealerorder;

import co.tz.tfc.ifmis.user.PortalUser;
import co.tz.tfc.ifmis.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A wholesale, non-subsidised inquiry or order from an agro-dealer to one TFC warehouse. */
@Entity
@Table(name = "dealer_orders")
@Getter
@Setter
@NoArgsConstructor
public class DealerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_id", nullable = false)
    private PortalUser dealer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DealerOrderKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DealerOrderStatus status = DealerOrderStatus.SUBMITTED;

    /** Sum of full (non-subsidised) prices at the time of the request — an estimate until the warehouse responds. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(length = 1000)
    private String dealerNote;

    @Column(length = 1000)
    private String warehouseResponse;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant respondedAt;

    private Long respondedBy;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DealerOrderItem> items = new ArrayList<>();
}
