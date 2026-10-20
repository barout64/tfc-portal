package co.tz.tfc.ifmis.warehouse;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String village;

    /** TFRA's own id for this warehouse, set once {@link #tfraLinkStatus} becomes LINKED. */
    private String tfraWarehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TfraLinkStatus tfraLinkStatus = TfraLinkStatus.PENDING;

    @Column(nullable = false)
    private boolean active = true;

    /** Whether a subsidised sale can currently be reserved at this warehouse. */
    public boolean canTakeSubsidisedSales() {
        return active && tfraLinkStatus == TfraLinkStatus.LINKED;
    }
}
