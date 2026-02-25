package com.nazir.inventory.repository;

import com.nazir.inventory.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void reserveStock_ShouldUpdateStock_WhenStockIsAvailable() {
        // Arrange
        Product product = Product.builder()
                .name("Test Product")
                .totalStock(10)
                .reservedStock(0)
                .build();
        productRepository.save(product);

        // Act
        int rowsAffected = productRepository.reserveStock(product.getId(), 5);

        // Assert
        assertThat(rowsAffected).isEqualTo(1);
        
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getReservedStock()).isEqualTo(5);
    }

    @Test
    void reserveStock_ShouldNotUpdateStock_WhenStockInsufficient() {
        // Arrange
        Product product = Product.builder()
                .name("Test Product")
                .totalStock(10)
                .reservedStock(8)
                .build();
        productRepository.save(product);

        // Act
        int rowsAffected = productRepository.reserveStock(product.getId(), 5);

        // Assert
        assertThat(rowsAffected).isEqualTo(0);
        
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getReservedStock()).isEqualTo(8);
    }
}
