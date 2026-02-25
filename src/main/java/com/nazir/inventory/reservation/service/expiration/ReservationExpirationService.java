package com.nazir.inventory.reservation.service.expiration;

import com.nazir.inventory.event.InventoryEvent;
import com.nazir.inventory.kafka.producer.InventoryEventProducer;
import com.nazir.inventory.product.repository.ProductRepository;
import com.nazir.inventory.reservation.entity.Reservation;
import com.nazir.inventory.reservation.entity.ReservationStatus;
import com.nazir.inventory.reservation.event.ReservationStockRestoredEvent;
import com.nazir.inventory.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryEventProducer inventoryEventProducer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processExpiration(Reservation reservation) {
        log.info("Processing expiration for Order ID: {}", reservation.getOrderId());
        int rows = productRepository.restoreStock(reservation.getProductId(), reservation.getQuantity());
        if (rows > 0) {
            eventPublisher.publishEvent(new ReservationStockRestoredEvent(reservation.getProductId(), reservation.getQuantity()));
        }
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);
        log.info("Expired reservation for Order ID: {}", reservation.getOrderId());
        
        // Publish event
        InventoryEvent event = InventoryEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("EXPIRED")
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .timestamp(Instant.now())
                .build();
        inventoryEventProducer.publishEvent(event);
    }
}
