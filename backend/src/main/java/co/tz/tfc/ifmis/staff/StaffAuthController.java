package co.tz.tfc.ifmis.staff;

import co.tz.tfc.ifmis.auth.JwtService;
import co.tz.tfc.ifmis.auth.dto.TokenResponse;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.staff.dto.StaffLoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal staff sign-in — ordinary username/password, separate from the farmer/agro-dealer OTP flow. */
@RestController
@RequestMapping("/api/auth/staff")
@RequiredArgsConstructor
public class StaffAuthController {

    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody StaffLoginRequest request) {
        StaffUser staff = staffUserRepository.findByUsernameAndActiveTrue(request.username())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));
        if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        String token = jwtService.issue("STAFF", staff.getId(), "STAFF");
        return new TokenResponse(token, staff.getDisplayName(), "STAFF");
    }
}
