package co.tz.tfc.ifmis.catalog.dto;

import co.tz.tfc.ifmis.catalog.AvailabilityLevel;
import co.tz.tfc.ifmis.catalog.Product;
import co.tz.tfc.ifmis.catalog.ProductType;

import java.math.BigDecimal;

/**
 * Public shape of a {@link Product}. The farmer price is only populated when the caller is an
 * authenticated, active farmer with room left in their entitlement for this product type —
 * never for an anonymous visitor or an agro-dealer.
 */
public record ProductSummary(
        Long id,
        Long warehouseId,
        String name,
        ProductType type,
        int bagSizeKg,
        BigDecimal fullPrice,
        BigDecimal farmerUnitPrice,
        AvailabilityLevel availability
) {
    public static ProductSummary of(Product product, boolean includeFarmerPrice) {
        return new ProductSummary(
                product.getId(),
                product.getWarehouse().getId(),
                product.getName(),
                product.getType(),
                product.getBagSizeKg(),
                product.getFullPrice(),
                includeFarmerPrice ? product.getFarmerUnitPrice() : null,
                product.availabilityLevel()
        );
    }
}
