package co.tz.tfc.ifmis.catalog;

import co.tz.tfc.ifmis.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    @Column(nullable = false)
    private int bagSizeKg;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fullPrice;

    /** Null when this product is not part of the subsidy programme (e.g. OTHER type, agro-dealer-only stock). */
    @Column(precision = 14, scale = 2)
    private BigDecimal farmerUnitPrice;

    /** Internal exact figure — never serialised directly to a portal response. */
    @Column(nullable = false)
    private int internalQuantity;

    @Column(nullable = false)
    private int lowStockThreshold = 20;

    @Column(nullable = false)
    private boolean active = true;

    public AvailabilityLevel availabilityLevel() {
        if (internalQuantity <= 0) {
            return AvailabilityLevel.OUT;
        }
        return internalQuantity <= lowStockThreshold ? AvailabilityLevel.LOW : AvailabilityLevel.AVAILABLE;
    }

    public boolean isSubsidised() {
        return farmerUnitPrice != null;
    }
}
