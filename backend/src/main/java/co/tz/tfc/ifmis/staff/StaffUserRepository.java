package co.tz.tfc.ifmis.staff;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUsernameAndActiveTrue(String username);
}
