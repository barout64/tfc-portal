package co.tz.tfc.ifmis.salerequest;

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

@Entity
@Table(name = "sale_requests")
@Getter
@Setter
@NoArgsConstructor
public class SaleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private PortalUser farmer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleRequestStatus status = SaleRequestStatus.SUBMITTED;

    /** Full (non-subsidised) market value of the reserved goods. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** What the farmer actually pays. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal farmerCashAmount;

    /** totalAmount - farmerCashAmount, the government's contribution. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subsidyAmount;

    @Column(nullable = false)
    private Instant reservedAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant completedAt;

    private Long completedBy;

    private Instant cancelledAt;

    /** TFRA's id for the pending sale, set when staff submit the bag QR codes (step 1 at the counter). */
    private String tfraPendingSaleId;

    private Instant tfraSubmittedAt;

    /** TFRA's final reference once the farmer's OTP confirmed the sale — shown on the receipt. */
    private String tfraSaleReference;

    @ElementCollection
    @CollectionTable(name = "sale_request_bag_qr_codes", joinColumns = @JoinColumn(name = "sale_request_id"))
    @Column(name = "qr_code", nullable = false, length = 120)
    private List<String> bagQrCodes = new ArrayList<>();

    @OneToMany(mappedBy = "saleRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleRequestItem> items = new ArrayList<>();

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public int totalBags() {
        return items.stream().mapToInt(SaleRequestItem::getBagCount).sum();
    }

    public boolean isSubmittedToTfra() {
        return tfraPendingSaleId != null;
    }

    public boolean isOpen() {
        return status == SaleRequestStatus.SUBMITTED || status == SaleRequestStatus.READY_FOR_PICKUP;
    }
}
