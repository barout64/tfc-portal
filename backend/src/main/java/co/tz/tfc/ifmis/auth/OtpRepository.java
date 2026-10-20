package co.tz.tfc.ifmis.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPortalUserIdAndPurposeOrderByIdDesc(Long portalUserId, OtpPurpose purpose);
}
