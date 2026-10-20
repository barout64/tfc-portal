package co.tz.tfc.ifmis.user;

public enum PortalUserStatus {
    /** TFRA identity confirmed; row created, but the phone OTP has not been verified yet. */
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED
}
