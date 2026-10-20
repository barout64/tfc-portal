package co.tz.tfc.ifmis.user;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.common.exception.NotFoundException;
import co.tz.tfc.ifmis.esb.TfraClient;
import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerSalesReport;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Agro-dealer self-service — requires an AGRO_DEALER JWT. */
@RestController
@RequestMapping("/api/agro-dealers/me")
@RequiredArgsConstructor
public class AgroDealerController {

    private final PortalUserRepository portalUserRepository;
    private final TfraClient tfraClient;

    @GetMapping("/sales-report")
    public TfraAgroDealerSalesReport salesReport(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        PortalUser dealer = portalUserRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Agro-dealer account not found."));
        return tfraClient.getAgroDealerSales(dealer.getTfraNumber(), from, to);
    }
}
