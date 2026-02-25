package com.nazir.inventory.dto;

import com.nazir.inventory.entity.ReservationStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private String orderId;
    private Long productId;
    private Integer quantity;
    private ReservationStatus status;
    private Instant expiresAt;
}
