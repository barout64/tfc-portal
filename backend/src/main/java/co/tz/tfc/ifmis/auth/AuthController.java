package co.tz.tfc.ifmis.auth;

import co.tz.tfc.ifmis.auth.dto.MaskedPhoneResponse;
import co.tz.tfc.ifmis.auth.dto.StartAuthRequest;
import co.tz.tfc.ifmis.auth.dto.TokenResponse;
import co.tz.tfc.ifmis.auth.dto.VerifyAuthRequest;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.user.PortalUserType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/{audience}")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public MaskedPhoneResponse register(@PathVariable String audience, @Valid @RequestBody StartAuthRequest request) {
        return authService.startRegistration(typeFor(audience), request.tfraNumber());
    }

    @PostMapping("/register/verify")
    public TokenResponse verifyRegistration(@PathVariable String audience, @Valid @RequestBody VerifyAuthRequest request) {
        return authService.completeRegistration(typeFor(audience), request.tfraNumber(), request.code());
    }

    @PostMapping("/login")
    public MaskedPhoneResponse login(@PathVariable String audience, @Valid @RequestBody StartAuthRequest request) {
        return authService.startLogin(typeFor(audience), request.tfraNumber());
    }

    @PostMapping("/login/verify")
    public TokenResponse verifyLogin(@PathVariable String audience, @Valid @RequestBody VerifyAuthRequest request) {
        return authService.completeLogin(typeFor(audience), request.tfraNumber(), request.code());
    }

    private PortalUserType typeFor(String audience) {
        return switch (audience) {
            case "farmers" -> PortalUserType.FARMER;
            case "agro-dealers" -> PortalUserType.AGRO_DEALER;
            default -> throw new ApiException(HttpStatus.NOT_FOUND, "Unknown audience: " + audience);
        };
    }
}
