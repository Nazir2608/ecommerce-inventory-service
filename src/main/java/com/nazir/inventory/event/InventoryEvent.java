package com.nazir.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private String eventId;
    private String eventType; // RESERVED, CONFIRMED, RELEASED, EXPIRED
    private String orderId;
    private Long productId;
    private Integer quantity;
    private Instant timestamp;
}
