package co.tz.tfc.ifmis.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Logs instead of sending a real SMS. Swap for an HTTP client against tfc-notification-gateway. */
@Slf4j
@Component
@ConditionalOnProperty(name = "tfc.notifications.provider", havingValue = "stub", matchIfMissing = true)
public class NotificationGatewayStub implements NotificationGateway {

    @Override
    public void sendSms(String phone, String message) {
        log.warn("[NotificationGatewayStub] SMS to {}: {}", phone, message);
    }
}
