package com.nazir.inventory.reservation.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservationStockRestoredEvent {
    private final Long productId;
    private final int quantity;
}
