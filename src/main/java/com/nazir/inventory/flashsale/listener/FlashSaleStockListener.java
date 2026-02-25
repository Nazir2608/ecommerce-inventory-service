package com.nazir.inventory.flashsale.listener;

import com.nazir.inventory.flashsale.service.RedisStockService;
import com.nazir.inventory.reservation.event.ReservationStockRestoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FlashSaleStockListener {

    private final RedisStockService redisStockService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStockRestored(ReservationStockRestoredEvent event) {
        log.debug("Received stock restored event for product: {}", event.getProductId());
        try {
            redisStockService.restoreStock(event.getProductId(), event.getQuantity());
        } catch (Exception e) {
            log.error("Failed to restore Redis stock for product: {}", event.getProductId(), e);
        }
    }
}
