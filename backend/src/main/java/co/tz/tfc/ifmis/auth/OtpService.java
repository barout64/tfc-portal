package co.tz.tfc.ifmis.auth;

import co.tz.tfc.ifmis.notification.NotificationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int VALID_MINUTES = 5;

    private final OtpRepository otpRepository;
    private final NotificationGateway notificationGateway;

    public void issue(Long portalUserId, String phone, OtpPurpose purpose) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

        OtpCode otp = new OtpCode();
        otp.setPortalUserId(portalUserId);
        otp.setPurpose(purpose);
        otp.setCodeHash(hash(code));
        otp.setExpiresAt(Instant.now().plus(VALID_MINUTES, ChronoUnit.MINUTES));
        otpRepository.save(otp);

        notificationGateway.sendSms(phone, "Your TFC portal verification code is " + code + ". It expires in " + VALID_MINUTES + " minutes.");
    }

    public boolean verify(Long portalUserId, OtpPurpose purpose, String submittedCode) {
        Optional<OtpCode> latest = otpRepository.findFirstByPortalUserIdAndPurposeOrderByIdDesc(portalUserId, purpose);
        if (latest.isEmpty()) {
            return false;
        }
        OtpCode otp = latest.get();
        if (!otp.isUsable()) {
            return false;
        }
        otp.setAttempts(otp.getAttempts() + 1);
        boolean matches = otp.getCodeHash().equals(hash(submittedCode));
        if (matches) {
            otp.setConsumedAt(Instant.now());
        }
        otpRepository.save(otp);
        return matches;
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
