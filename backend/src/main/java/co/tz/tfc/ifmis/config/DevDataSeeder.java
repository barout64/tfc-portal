package co.tz.tfc.ifmis.config;

import co.tz.tfc.ifmis.catalog.Product;
import co.tz.tfc.ifmis.catalog.ProductRepository;
import co.tz.tfc.ifmis.catalog.ProductType;
import co.tz.tfc.ifmis.staff.StaffRole;
import co.tz.tfc.ifmis.staff.StaffUser;
import co.tz.tfc.ifmis.staff.StaffUserRepository;
import co.tz.tfc.ifmis.warehouse.TfraLinkStatus;
import co.tz.tfc.ifmis.warehouse.Warehouse;
import co.tz.tfc.ifmis.warehouse.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Dev/demo convenience data — a linked warehouse, a few products, and one staff login. Never runs against an existing database. */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (warehouseRepository.count() > 0) {
            return;
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName("Morogoro Central Warehouse");
        warehouse.setRegion("Morogoro");
        warehouse.setDistrict("Morogoro Urban");
        warehouse.setVillage("Kihonda");
        warehouse.setTfraWarehouseId("TFRA-WH-0001");
        warehouse.setTfraLinkStatus(TfraLinkStatus.LINKED);
        warehouse = warehouseRepository.save(warehouse);

        productRepository.save(product(warehouse, "DAP Fertilizer 50kg", ProductType.FERTILIZER, 50, "72000", "48000", 500, 20));
        productRepository.save(product(warehouse, "Urea Fertilizer 50kg", ProductType.FERTILIZER, 50, "68000", "45000", 15, 20));
        productRepository.save(product(warehouse, "Maize Seed (hybrid) 10kg", ProductType.SEEDS, 10, "35000", "20000", 0, 10));

        // Wholesale-only stock (no farmer price) — agro-dealers can order it, farmers can't reserve it.
        Product wholesale = product(warehouse, "NPK 17:17:17 50kg (wholesale)", ProductType.FERTILIZER, 50, "75000", null, 300, 20);
        productRepository.save(wholesale);

        // A second warehouse, still pending TFRA linking, to exercise the location filters and staff scoping.
        Warehouse mbeya = new Warehouse();
        mbeya.setName("Mbeya Depot");
        mbeya.setRegion("Mbeya");
        mbeya.setDistrict("Mbeya City");
        mbeya.setVillage("Iyunga");
        mbeya.setTfraLinkStatus(TfraLinkStatus.PENDING);
        mbeya = warehouseRepository.save(mbeya);
        productRepository.save(product(mbeya, "CAN Fertilizer 50kg", ProductType.FERTILIZER, 50, "66000", "44000", 120, 20));

        staffUserRepository.save(staff("warehouse1", "Morogoro Central Warehouse Staff", warehouse));
        staffUserRepository.save(staff("warehouse2", "Mbeya Depot Staff", mbeya));

        log.warn("[DevDataSeeder] Seeded demo data. Staff logins: warehouse1 / warehouse2, password ChangeMe123! — change before any real deployment.");
    }

    private StaffUser staff(String username, String displayName, Warehouse warehouse) {
        StaffUser staff = new StaffUser();
        staff.setUsername(username);
        staff.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
        staff.setDisplayName(displayName);
        staff.setRole(StaffRole.WAREHOUSE_STAFF);
        staff.setWarehouse(warehouse);
        return staff;
    }

    private Product product(Warehouse warehouse, String name, ProductType type, int bagSizeKg, String fullPrice, String farmerPrice, int quantity, int lowStockThreshold) {
        Product product = new Product();
        product.setWarehouse(warehouse);
        product.setName(name);
        product.setType(type);
        product.setBagSizeKg(bagSizeKg);
        product.setFullPrice(new BigDecimal(fullPrice));
        product.setFarmerUnitPrice(farmerPrice == null ? null : new BigDecimal(farmerPrice));
        product.setInternalQuantity(quantity);
        product.setLowStockThreshold(lowStockThreshold);
        return product;
    }
}
