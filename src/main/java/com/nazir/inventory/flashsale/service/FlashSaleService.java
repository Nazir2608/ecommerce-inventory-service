package com.nazir.inventory.flashsale.service;

import com.nazir.inventory.exception.OutOfStockException;
import com.nazir.inventory.reservation.dto.ReservationRequest;
import com.nazir.inventory.reservation.dto.ReservationResponse;
import com.nazir.inventory.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleService {

    private final RedisStockService redisStockService;
    private final ReservationService reservationService;

    public ReservationResponse reserveFlashSale(ReservationRequest request) {
        log.info("Processing flash sale reservation for Product ID: {}", request.getProductId());

        // 1. Redis Pre-check (Fast Reject)
        boolean acquired = redisStockService.decrementStock(request.getProductId(), request.getQuantity());
        if (!acquired) {
            log.warn("Flash sale stock exhausted in Redis for Product ID: {}", request.getProductId());
            throw new OutOfStockException("Flash sale stock exhausted for product: " + request.getProductId());
        }

        try {
            // 2. DB Atomic Update (Source of Truth)
            return reservationService.reserveInventory(request);
        } catch (Exception e) {
            // 3. Compensation: Restore Redis stock if DB fails
            log.error("DB reservation failed. Compensating Redis stock for Product ID: {}", request.getProductId(), e);
            redisStockService.restoreStock(request.getProductId(), request.getQuantity());
            throw e;
        }
    }
}