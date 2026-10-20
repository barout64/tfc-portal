package co.tz.tfc.ifmis.catalog;

/**
 * Coarse stock signal shown to portal users. The exact quantity is an internal figure and is
 * never exposed here — see {@link Product#availabilityLevel()}.
 */
public enum AvailabilityLevel {
    AVAILABLE,
    LOW,
    OUT
}
