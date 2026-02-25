package com.nazir.inventory.product.service;

import com.nazir.inventory.exception.ResourceNotFoundException;
import com.nazir.inventory.product.dto.ProductRequest;
import com.nazir.inventory.product.dto.ProductResponse;
import com.nazir.inventory.product.entity.Product;
import com.nazir.inventory.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with name: {}", request.getName());
        Product product = Product.builder()
                .name(request.getName())
                .totalStock(request.getTotalStock())
                .reservedStock(0)
                .availableStock(request.getTotalStock())
                .build();
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        return mapToProductResponse(product);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .totalStock(product.getTotalStock())
                .reservedStock(product.getReservedStock())
                .availableStock(product.getAvailableStock())
                .build();
    }
}