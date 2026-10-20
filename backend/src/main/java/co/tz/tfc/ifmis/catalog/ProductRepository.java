package co.tz.tfc.ifmis.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueAndWarehouse_Id(Long warehouseId);

    List<Product> findByActiveTrueAndType(ProductType type);
}
