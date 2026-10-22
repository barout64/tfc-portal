package co.tz.tfc.ifmis.catalog;

import co.tz.tfc.ifmis.auth.AuthenticatedPrincipal;
import co.tz.tfc.ifmis.catalog.dto.ProductSummary;
import co.tz.tfc.ifmis.catalog.dto.WarehouseSummary;
import co.tz.tfc.ifmis.warehouse.Warehouse;
import co.tz.tfc.ifmis.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public, read-mostly catalogue browse. Anonymous visitors and agro-dealers never see the farmer
 * (subsidised) price for a product — only an authenticated, active farmer does.
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    /** Optional region / district / village filters, each an exact, case-insensitive match. */
    @GetMapping("/warehouses")
    public List<WarehouseSummary> warehouses(@RequestParam(required = false) String region,
                                             @RequestParam(required = false) String district,
                                             @RequestParam(required = false) String village) {
        boolean includeFarmerPrice = currentUserIsActiveFarmer();
        return warehouseRepository.findByActiveTrue().stream()
                .filter(w -> matches(region, w.getRegion()) && matches(district, w.getDistrict()) && matches(village, w.getVillage()))
                .map(w -> WarehouseSummary.of(w, productsFor(w.getId(), includeFarmerPrice)))
                .toList();
    }

    /** Distinct locations of active warehouses, so the portal can offer region → district → village pickers. */
    @GetMapping("/locations")
    public List<Location> locations() {
        return warehouseRepository.findByActiveTrue().stream()
                .map(w -> new Location(w.getRegion(), w.getDistrict(), w.getVillage()))
                .distinct()
                .sorted(java.util.Comparator.comparing(Location::region)
                        .thenComparing(l -> l.district() == null ? "" : l.district())
                        .thenComparing(l -> l.village() == null ? "" : l.village()))
                .toList();
    }

    public record Location(String region, String district, String village) {
    }

    @GetMapping("/warehouses/{id}")
    public WarehouseSummary warehouse(@PathVariable Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .filter(Warehouse::isActive)
                .orElseThrow(() -> new co.tz.tfc.ifmis.common.exception.NotFoundException("Warehouse not found."));
        return WarehouseSummary.of(warehouse, productsFor(id, currentUserIsActiveFarmer()));
    }

    private List<ProductSummary> productsFor(Long warehouseId, boolean includeFarmerPrice) {
        return productRepository.findByActiveTrueAndWarehouse_Id(warehouseId).stream()
                .map(p -> ProductSummary.of(p, includeFarmerPrice && p.isSubsidised()))
                .toList();
    }

    private static boolean matches(String filter, String value) {
        return filter == null || filter.isBlank() || filter.trim().equalsIgnoreCase(value);
    }

    private boolean currentUserIsActiveFarmer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal
                && principal.isFarmer();
    }
}
