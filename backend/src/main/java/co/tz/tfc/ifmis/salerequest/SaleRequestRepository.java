package co.tz.tfc.ifmis.salerequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleRequestRepository extends JpaRepository<SaleRequest, Long> {

    Optional<SaleRequest> findByReference(String reference);

    List<SaleRequest> findByFarmer_IdOrderByReservedAtDesc(Long farmerId);

    List<SaleRequest> findByFarmer_IdAndStatusIn(Long farmerId, List<SaleRequestStatus> statuses);

    List<SaleRequest> findByStatusInAndExpiresAtBefore(List<SaleRequestStatus> statuses, java.time.Instant cutoff);
}
