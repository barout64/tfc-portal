package co.tz.tfc.ifmis.esb;

import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerProfile;
import co.tz.tfc.ifmis.esb.dto.TfraAgroDealerSalesReport;
import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Placeholder for the real tfc-esb-gw-backed {@link TfraClient}. Returns canned, obviously-fake
 * data so the rest of the portal (registration, catalogue, sale requests) can be built and
 * tested before the real Gov-ESB/TFRA integration is wired in.
 *
 * <p>Swap this out by implementing {@link TfraClient} against the real gateway and setting
 * {@code tfc.esb.provider=live} (or simply removing this bean / the stub condition).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tfc.esb.provider", havingValue = "stub", matchIfMissing = true)
public class TfraClientStub implements TfraClient {

    @Override
    public Optional<TfraFarmerDetails> getFarmerDetails(String farmerNumber) {
        log.warn("[TfraClientStub] getFarmerDetails({}) — using stub TFRA data, not a real lookup", farmerNumber);
        if (farmerNumber == null || farmerNumber.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TfraFarmerDetails(
                farmerNumber,
                "Stub Farmer " + farmerNumber,
                "+2557" + String.format("%08d", Math.abs(farmerNumber.hashCode()) % 100000000),
                true,
                false,
                Map.of("FERTILIZER", 150, "SEEDS", 20),
                List.of(),
                List.of()
        ));
    }

    @Override
    public Optional<TfraAgroDealerProfile> verifyAgroDealer(String tfraDealerNumber) {
        log.warn("[TfraClientStub] verifyAgroDealer({}) — using stub TFRA data, not a real lookup", tfraDealerNumber);
        if (tfraDealerNumber == null || tfraDealerNumber.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TfraAgroDealerProfile(
                tfraDealerNumber,
                "Stub Agro-Dealer " + tfraDealerNumber,
                "+2557" + String.format("%08d", Math.abs(tfraDealerNumber.hashCode()) % 100000000),
                true
        ));
    }

    @Override
    public Optional<String> submitSale(String tfraWarehouseId, String farmerNumber, String portalReference, List<String> bagQrCodes) {
        log.warn("[TfraClientStub] submitSale({}, {}, {}, {} codes) — accepted without real TFRA validation",
                tfraWarehouseId, farmerNumber, portalReference, bagQrCodes.size());
        if (bagQrCodes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("STUB-PENDING-" + UUID.randomUUID());
    }

    @Override
    public String requestOtp(String tfraWarehouseId, String farmerNumber, String tfraSaleId) {
        String requestId = UUID.randomUUID().toString();
        log.warn("[TfraClientStub] requestOtp({}, {}, {}) -> requestId={} — no real SMS was sent by TFRA; any non-blank OTP is accepted",
                tfraWarehouseId, farmerNumber, tfraSaleId, requestId);
        return requestId;
    }

    @Override
    public Optional<String> confirmSale(String tfraWarehouseId, String tfraSaleId, String otp) {
        log.warn("[TfraClientStub] confirmSale({}, {}, otp={}) — accepted without real TFRA confirmation", tfraWarehouseId, tfraSaleId, otp);
        if (otp == null || otp.isBlank()) {
            return Optional.empty();
        }
        return Optional.of("STUB-TFRA-" + UUID.randomUUID());
    }

    @Override
    public TfraAgroDealerSalesReport getAgroDealerSales(String tfraDealerNumber, LocalDate from, LocalDate to) {
        log.warn("[TfraClientStub] getAgroDealerSales({}, {}, {}) — using stub TFRA data, not a real report", tfraDealerNumber, from, to);
        return new TfraAgroDealerSalesReport(tfraDealerNumber, from, to, 0, 0, 0, List.of());
    }
}
