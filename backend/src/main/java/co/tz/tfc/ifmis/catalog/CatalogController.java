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

    @GetMapping("/warehouses")
    public List<WarehouseSummary> warehouses(@RequestParam(required = false) String region) {
        boolean includeFarmerPrice = currentUserIsActiveFarmer();
        List<Warehouse> warehouses = region == null || region.isBlank()
                ? warehouseRepository.findByActiveTrue()
                : warehouseRepository.findByActiveTrueAndRegionIgnoreCase(region);

        return warehouses.stream()
                .map(w -> WarehouseSummary.of(w, productsFor(w.getId(), includeFarmerPrice)))
                .toList();
    }

    @GetMapping("/warehouses/{id}")
    public WarehouseSummary warehouse(@PathVariable Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new co.tz.tfc.ifmis.common.exception.NotFoundException("Warehouse not found."));
        return WarehouseSummary.of(warehouse, productsFor(id, currentUserIsActiveFarmer()));
    }

    private List<ProductSummary> productsFor(Long warehouseId, boolean includeFarmerPrice) {
        return productRepository.findByActiveTrueAndWarehouse_Id(warehouseId).stream()
                .map(p -> ProductSummary.of(p, includeFarmerPrice && p.isSubsidised()))
                .toList();
    }

    private boolean currentUserIsActiveFarmer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal
                && principal.isFarmer();
    }
}
