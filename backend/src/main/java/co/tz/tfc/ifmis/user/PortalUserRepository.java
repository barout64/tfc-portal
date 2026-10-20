package co.tz.tfc.ifmis.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByTypeAndTfraNumber(PortalUserType type, String tfraNumber);

    Optional<PortalUser> findByPhone(String phone);

    boolean existsByTypeAndTfraNumber(PortalUserType type, String tfraNumber);
}
