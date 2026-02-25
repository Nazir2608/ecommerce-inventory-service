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
    @Query("UPDATE Product p SET p.reservedStock = p.reservedStock + :qty WHERE p.id = :productId AND (p.totalStock - p.reservedStock) >= :qty")
    int reserveStock(@Param("productId") Long productId, @Param("qty") int qty);
}
