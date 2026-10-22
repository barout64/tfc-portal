package co.tz.tfc.ifmis.dealerorder;

import co.tz.tfc.ifmis.catalog.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "dealer_order_items")
@Getter
@Setter
@NoArgsConstructor
public class DealerOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_order_id", nullable = false)
    private DealerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int bagCount;

    /** Full price snapshotted when the dealer sent the request. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;
}
