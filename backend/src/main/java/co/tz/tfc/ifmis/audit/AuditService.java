package co.tz.tfc.ifmis.audit;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    public void record(String action, String subjectType, Long subjectId) {
        record(action, subjectType, subjectId, null);
    }

    public void record(String action, String subjectType, Long subjectId, Long warehouseId) {
        AuditEvent event = new AuditEvent();
        event.setAction(action);
        event.setSubjectType(subjectType);
        event.setSubjectId(subjectId);
        event.setActor(currentActor());
        event.setWarehouseId(warehouseId);
        repository.save(event);
    }

    private String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return principal.subjectType() + ":" + principal.id();
        }
        return null;
    }
}
