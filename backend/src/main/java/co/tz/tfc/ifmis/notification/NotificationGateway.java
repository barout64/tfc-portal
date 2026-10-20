package co.tz.tfc.ifmis.notification;

/** Thin seam over {@code tfc-notification-gateway} for SMS to farmers/agro-dealers. */
public interface NotificationGateway {

    void sendSms(String phone, String message);
}
