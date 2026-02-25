package com.nazir.inventory.repository;

import com.nazir.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock + :qty, p.availableStock = p.availableStock - :qty WHERE p.id = :productId AND p.availableStock >= :qty")
    int reserveStock(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock - :qty, p.availableStock = p.availableStock + :qty WHERE p.id = :productId AND p.reservedStock >= :qty")
    int restoreStock(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock - :qty, p.totalStock = p.totalStock - :qty WHERE p.id = :productId AND p.reservedStock >= :qty")
    int confirmStock(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.totalStock = p.totalStock + :qty, p.availableStock = p.availableStock + :qty WHERE p.id = :productId")
    int restockProduct(@Param("productId") Long productId, @Param("qty") int qty);
}
