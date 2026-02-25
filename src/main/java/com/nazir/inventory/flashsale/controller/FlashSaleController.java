package com.nazir.inventory.flashsale.controller;

import com.nazir.inventory.common.dto.ApiResponse;
import com.nazir.inventory.flashsale.service.FlashSaleService;
import com.nazir.inventory.flashsale.service.RedisStockService;
import com.nazir.inventory.reservation.dto.ReservationRequest;
import com.nazir.inventory.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flash-sale")
@RequiredArgsConstructor
@Slf4j
public class FlashSaleController {

    private final RedisStockService redisStockService;
    private final FlashSaleService flashSaleService;

    @PostMapping("/products/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enableFlashSale(@PathVariable Long id) {
        log.info("Enabling flash sale for product ID: {}", id);
        redisStockService.enableFlashSale(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Flash sale enabled successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @PostMapping("/products/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableFlashSale(@PathVariable Long id) {
        log.info("Disabling flash sale for product ID: {}", id);
        redisStockService.disableFlashSale(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Flash sale disabled successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveFlashSale(@Valid @RequestBody ReservationRequest request) {
        log.info("Received flash sale reservation request: {}", request);
        ReservationResponse response = flashSaleService.reserveFlashSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReservationResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Flash sale inventory reserved successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }
}