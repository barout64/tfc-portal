package co.tz.tfc.ifmis.auth;

/** What ends up in the {@code SecurityContext} for a request bearing a valid portal JWT. */
public record AuthenticatedPrincipal(String subjectType, Long id, String role) {

    public boolean isFarmer() {
        return "FARMER".equals(role);
    }

    public boolean isAgroDealer() {
        return "AGRO_DEALER".equals(role);
    }

    public boolean isStaff() {
        return "STAFF".equals(role);
    }
}
