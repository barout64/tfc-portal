package co.tz.tfc.ifmis.dealerorder;

/**
 * INQUIRY: SUBMITTED → ANSWERED. ORDER: SUBMITTED → ACCEPTED → FULFILLED, or SUBMITTED → REJECTED.
 * The dealer can CANCEL either kind while it is still SUBMITTED.
 */
public enum DealerOrderStatus {
    SUBMITTED,
    ANSWERED,
    ACCEPTED,
    REJECTED,
    FULFILLED,
    CANCELLED
}
