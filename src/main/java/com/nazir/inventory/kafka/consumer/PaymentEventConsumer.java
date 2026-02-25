package com.nazir.inventory.kafka.consumer;

import com.nazir.inventory.config.KafkaConfig;
import com.nazir.inventory.event.PaymentEvent;
import com.nazir.inventory.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ReservationService reservationService;

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void consumePaymentEvent(PaymentEvent event) {
        if (event == null) {
            log.warn("Received null payment event (possibly deserialization error). Skipping.");
            return;
        }
        log.info("Received payment event: {}", event);
        
        try {
            if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
                reservationService.confirmReservation(event.getOrderId());
            } else if ("FAILED".equalsIgnoreCase(event.getStatus())) {
                reservationService.cancelReservation(event.getOrderId());
            } else {
                log.warn("Unknown payment status: {}", event.getStatus());
            }
        } catch (Exception e) {
            log.error("Error processing payment event for Order ID: {}", event.getOrderId(), e);
            // Kafka will retry based on configuration
        }
    }
}
