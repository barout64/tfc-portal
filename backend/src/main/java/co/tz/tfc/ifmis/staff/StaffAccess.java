package co.tz.tfc.ifmis.staff;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.common.exception.ApiException;
import co.tz.tfc.ifmis.warehouse.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Scopes warehouse staff to their own warehouse: a WAREHOUSE_STAFF user can only see and act on
 * requests and orders for the warehouse they are assigned to. ADMIN can act on any warehouse.
 */
@Component
@RequiredArgsConstructor
public class StaffAccess {

    private final StaffUserRepository staffUserRepository;

    public StaffUser current(AuthenticatedPrincipal principal) {
        StaffUser staff = staffUserRepository.findById(principal.id())
                .filter(StaffUser::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Staff account not found or inactive."));
        if (staff.getRole() != StaffRole.ADMIN && staff.getWarehouse() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your staff account is not assigned to a warehouse.");
        }
        return staff;
    }

    public void requireWarehouse(StaffUser staff, Warehouse warehouse) {
        if (staff.getRole() == StaffRole.ADMIN) {
            return;
        }
        if (!staff.getWarehouse().getId().equals(warehouse.getId())) {
            // Same message as a missing reference so staff can't probe other warehouses' references.
            throw new ApiException(HttpStatus.NOT_FOUND, "Not found at your warehouse.");
        }
    }
}
