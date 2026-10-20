package co.tz.tfc.ifmis.auth;

import co.tz.tfc.ifmis.audit.AuditService;
import co.tz.tfc.ifmis.auth.dto.MaskedPhoneResponse;
import co.tz.tfc.ifmis.auth.dto.TokenResponse;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.esb.TfraClient;
import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerProfile;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import co.tz.tfc.ifmis.user.PortalUser;
import co.tz.tfc.ifmis.user.PortalUserRepository;
import co.tz.tfc.ifmis.user.PortalUserStatus;
import co.tz.tfc.ifmis.user.PortalUserType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PortalUserRepository portalUserRepository;
    private final OtpService otpService;
    private final TfraClient tfraClient;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public MaskedPhoneResponse startRegistration(PortalUserType type, String tfraNumber) {
        if (portalUserRepository.existsByTypeAndTfraNumber(type, tfraNumber)) {
            throw new ApiException(HttpStatus.CONFLICT, "This TFRA number is already registered — try logging in instead.");
        }

        PortalUser user = new PortalUser();
        user.setType(type);
        user.setTfraNumber(tfraNumber);

        if (type == PortalUserType.FARMER) {
            TfraFarmerDetails details = tfraClient.getFarmerDetails(tfraNumber)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TFRA has no farmer with this wallet number."));
            if (!details.active() || details.stopSubsidy()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This farmer is not currently eligible for the subsidy programme.");
            }
            user.setPhone(details.phoneOnFile());
            user.setDisplayName(details.fullName());
        } else {
            TfraAgroDealerProfile profile = tfraClient.verifyAgroDealer(tfraNumber)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TFRA has no agro-dealer with this number."));
            if (!profile.active()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This agro-dealer is not currently active with TFRA.");
            }
            user.setPhone(profile.phoneOnFile());
            user.setDisplayName(profile.businessName());
        }

        user = portalUserRepository.save(user);
        otpService.issue(user.getId(), user.getPhone(), OtpPurpose.REGISTER);
        auditService.record("PORTAL_REGISTRATION_STARTED", type.name(), user.getId());

        return new MaskedPhoneResponse("A verification code was sent to the phone TFRA has on file.", mask(user.getPhone()));
    }

    @Transactional
    public TokenResponse completeRegistration(PortalUserType type, String tfraNumber, String code) {
        PortalUser user = portalUserRepository.findByTypeAndTfraNumber(type, tfraNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Start registration first."));
        if (user.getStatus() != PortalUserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.CONFLICT, "This account is already verified — try logging in instead.");
        }
        if (!otpService.verify(user.getId(), OtpPurpose.REGISTER, code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That code is incorrect or has expired.");
        }

        user.setStatus(PortalUserStatus.ACTIVE);
        user.setVerifiedAt(Instant.now());
        portalUserRepository.save(user);
        auditService.record("PORTAL_REGISTRATION_COMPLETED", type.name(), user.getId());

        return issueToken(user);
    }

    @Transactional
    public MaskedPhoneResponse startLogin(PortalUserType type, String tfraNumber) {
        PortalUser user = activeUser(type, tfraNumber);
        otpService.issue(user.getId(), user.getPhone(), OtpPurpose.LOGIN);
        auditService.record("PORTAL_LOGIN_OTP_SENT", type.name(), user.getId());
        return new MaskedPhoneResponse("A login code was sent to your phone.", mask(user.getPhone()));
    }

    @Transactional
    public TokenResponse completeLogin(PortalUserType type, String tfraNumber, String code) {
        PortalUser user = activeUser(type, tfraNumber);
        if (!otpService.verify(user.getId(), OtpPurpose.LOGIN, code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That code is incorrect or has expired.");
        }
        auditService.record("PORTAL_LOGIN_COMPLETED", type.name(), user.getId());
        return issueToken(user);
    }

    private PortalUser activeUser(PortalUserType type, String tfraNumber) {
        PortalUser user = portalUserRepository.findByTypeAndTfraNumber(type, tfraNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No account for this TFRA number — register first."));
        if (user.getStatus() == PortalUserStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been suspended.");
        }
        if (user.getStatus() == PortalUserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.CONFLICT, "Finish registration first.");
        }
        return user;
    }

    private TokenResponse issueToken(PortalUser user) {
        String token = jwtService.issue(user.getType().name(), user.getId(), user.getType().name());
        return new TokenResponse(token, user.getDisplayName(), user.getType().name());
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
