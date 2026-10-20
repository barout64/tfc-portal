package co.tz.tfc.ifmis.salerequest;

import co.tz.tfc.ifmis.catalog.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_request_items")
@Getter
@Setter
@NoArgsConstructor
public class SaleRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_request_id", nullable = false)
    private SaleRequest saleRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int bagCount;

    /** Prices snapshotted at reservation time so a later price change never alters an existing reservation. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal fullUnitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal farmerUnitPrice;
}
