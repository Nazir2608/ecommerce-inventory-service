package com.nazir.inventory.product.controller;

import com.nazir.inventory.common.dto.ApiResponse;
import com.nazir.inventory.product.dto.ProductRequest;
import com.nazir.inventory.product.dto.ProductResponse;
import com.nazir.inventory.product.service.ProductService;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("Received request to create product: {}", request);
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Product created successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        log.info("Received request to fetch product with ID: {}", id);
        ProductResponse response = productService.getProduct(id);
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Product retrieved successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }
}