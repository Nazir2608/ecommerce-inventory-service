package com.nazir.inventory.flashsale.controller;

import com.nazir.inventory.flashsale.service.FlashSaleService;
import com.nazir.inventory.reservation.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class FlashSaleLoadTestController {

    private final FlashSaleService flashSaleService;

    @PostMapping("/flash-load/{productId}")
    public Map<String, Object> simulateLoad(@PathVariable Long productId) throws InterruptedException {

        int totalRequests = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        Map<String, AtomicInteger> exceptionCounts = new ConcurrentHashMap<>();

        for (int i = 0; i < totalRequests; i++) {
            int orderNumber = i;

            executor.submit(() -> {
                try {
                    flashSaleService.reserveFlashSale(new ReservationRequest("LOADS-" + orderNumber, productId, 1));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failure.incrementAndGet();
                    String errorKey = e.getClass().getSimpleName() + ": " + e.getMessage();
                    exceptionCounts.computeIfAbsent(errorKey, k -> new AtomicInteger()).incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        
        log.info("Load test completed. Success: {}, Failure: {}", success.get(), failure.get());
        exceptionCounts.forEach((k, v) -> log.error("Exception: {} -> Count: {}", k, v.get()));

        return Map.of(
            "totalRequests", totalRequests, 
            "success", success.get(), 
            "failure", failure.get(),
            "exceptions", exceptionCounts
        );
    }
}
