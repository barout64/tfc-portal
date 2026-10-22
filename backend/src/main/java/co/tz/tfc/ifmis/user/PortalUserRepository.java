package co.tz.tfc.ifmis.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByTypeAndTfraNumber(PortalUserType type, String tfraNumber);

    Optional<PortalUser> findByPhone(String phone);

    /** Row-locks the user so one farmer's concurrent reservations are checked against entitlement one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from PortalUser u where u.id = :id")
    Optional<PortalUser> findForUpdate(@Param("id") Long id);

    boolean existsByTypeAndTfraNumber(PortalUserType type, String tfraNumber);
}
