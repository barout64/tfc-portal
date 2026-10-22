package co.tz.tfc.ifmis.dealerorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DealerOrderRepository extends JpaRepository<DealerOrder, Long> {

    Optional<DealerOrder> findByReference(String reference);

    List<DealerOrder> findByDealer_IdOrderByCreatedAtDesc(Long dealerId);

    List<DealerOrder> findByWarehouse_IdOrderByCreatedAtDesc(Long warehouseId);

    List<DealerOrder> findAllByOrderByCreatedAtDesc();
}
