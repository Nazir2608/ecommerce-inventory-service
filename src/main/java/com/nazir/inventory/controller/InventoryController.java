package com.nazir.inventory.controller;

import com.nazir.inventory.common.dto.ApiResponse;
import com.nazir.inventory.dto.ProductRequest;
import com.nazir.inventory.dto.ProductResponse;
import com.nazir.inventory.dto.ReservationRequest;
import com.nazir.inventory.dto.ReservationResponse;
import com.nazir.inventory.service.InventoryService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("Received request to create product: {}", request);
        ProductResponse response = inventoryService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Product created successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        log.info("Received request to fetch product with ID: {}", id);
        ProductResponse response = inventoryService.getProduct(id);
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Product retrieved successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveInventory(@Valid @RequestBody ReservationRequest request) {
        log.info("Received inventory reservation request: {}", request);
        ReservationResponse response = inventoryService.reserveInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReservationResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Inventory reserved successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }
}
