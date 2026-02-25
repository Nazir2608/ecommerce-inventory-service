package com.nazir.inventory.kafka.producer;

import com.nazir.inventory.config.KafkaConfig;
import com.nazir.inventory.event.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    public void publishEvent(InventoryEvent event) {
        log.info("Publishing inventory event: {}", event);
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_INVENTORY_EVENTS, event.getOrderId(), event);
        } catch (Exception e) {
            log.error("Failed to publish inventory event for Order ID: {}", event.getOrderId(), e);
            // In a real system, you might want to retry or store in an outbox table
        }
    }
}
