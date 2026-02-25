package com.nazir.inventory.reservation.service;

import com.nazir.inventory.reservation.dto.ReservationRequest;
import com.nazir.inventory.reservation.dto.ReservationResponse;

public interface ReservationService {
    ReservationResponse reserveInventory(ReservationRequest request);
    ReservationResponse confirmReservation(String orderId);
    ReservationResponse releaseReservation(String orderId);
    ReservationResponse cancelReservation(String orderId);
    void expireReservations();
}