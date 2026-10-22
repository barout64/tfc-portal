package co.tz.tfc.ifmis.farmer;

import co.tz.tfc.ifmis.esb.dto.TfraFarmerDetails;
import co.tz.tfc.ifmis.salerequest.SaleRequest;
import co.tz.tfc.ifmis.salerequest.SaleRequestItem;
import co.tz.tfc.ifmis.salerequest.SaleRequestRepository;
import co.tz.tfc.ifmis.salerequest.SaleRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Remaining entitlement per product type, in kg:
 * {@code entitled (TFRA) - received (TFRA) - held by this farmer's open portal reservations}.
 *
 * <p>Completed portal sales are not subtracted separately: once confirmed with TFRA they appear in
 * TFRA's received items, so counting them here too would subtract them twice.
 */
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private static final List<SaleRequestStatus> OPEN = List.of(SaleRequestStatus.SUBMITTED, SaleRequestStatus.READY_FOR_PICKUP);

    private final SaleRequestRepository saleRequestRepository;

    public record Line(String type, int entitledKg, int receivedKg, int reservedKg, int remainingKg) {
    }

    public Map<String, Line> summarise(Long farmerId, TfraFarmerDetails details) {
        Map<String, Integer> received = new HashMap<>();
        for (TfraFarmerDetails.ReceivedItem item : details.receivedItems()) {
            received.merge(item.productType(), item.quantityKg(), Integer::sum);
        }

        Map<String, Integer> reserved = new HashMap<>();
        for (SaleRequest request : saleRequestRepository.findByFarmer_IdAndStatusIn(farmerId, OPEN)) {
            if (request.isExpired()) {
                continue;
            }
            for (SaleRequestItem item : request.getItems()) {
                reserved.merge(item.getProduct().getType().name(), item.getProduct().getBagSizeKg() * item.getBagCount(), Integer::sum);
            }
        }

        Map<String, Line> lines = new TreeMap<>();
        details.entitlementByType().forEach((type, entitled) -> {
            int receivedKg = received.getOrDefault(type, 0);
            int reservedKg = reserved.getOrDefault(type, 0);
            lines.put(type, new Line(type, entitled, receivedKg, reservedKg, Math.max(0, entitled - receivedKg - reservedKg)));
        });
        return lines;
    }
}
