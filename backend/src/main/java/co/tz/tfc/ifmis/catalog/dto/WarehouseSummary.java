package co.tz.tfc.ifmis.catalog.dto;

import co.tz.tfc.ifmis.warehouse.Warehouse;

import java.util.List;

public record WarehouseSummary(
        Long id,
        String name,
        String region,
        String district,
        String village,
        boolean canTakeSubsidisedSales,
        List<ProductSummary> products
) {
    public static WarehouseSummary of(Warehouse warehouse, List<ProductSummary> products) {
        return new WarehouseSummary(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getRegion(),
                warehouse.getDistrict(),
                warehouse.getVillage(),
                warehouse.canTakeSubsidisedSales(),
                products
        );
    }
}
