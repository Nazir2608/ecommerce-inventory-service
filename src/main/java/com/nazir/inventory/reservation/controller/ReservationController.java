package com.nazir.inventory.reservation.controller;

import com.nazir.inventory.common.dto.ApiResponse;
import com.nazir.inventory.reservation.dto.ReservationRequest;
import com.nazir.inventory.reservation.dto.ReservationResponse;
import com.nazir.inventory.reservation.service.ReservationService;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveInventory(@Valid @RequestBody ReservationRequest request) {
        log.info("Received inventory reservation request: {}", request);
        ReservationResponse response = reservationService.reserveInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ReservationResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Inventory reserved successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirmReservation(@PathVariable String orderId) {
        log.info("Received request to confirm reservation for Order ID: {}", orderId);
        ReservationResponse response = reservationService.confirmReservation(orderId);
        return ResponseEntity.ok(
                ApiResponse.<ReservationResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Reservation confirmed successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @PostMapping("/{orderId}/release")
    public ResponseEntity<ApiResponse<ReservationResponse>> releaseReservation(@PathVariable String orderId) {
        log.info("Received request to release reservation for Order ID: {}", orderId);
        ReservationResponse response = reservationService.releaseReservation(orderId);
        return ResponseEntity.ok(
                ApiResponse.<ReservationResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Reservation released successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(@PathVariable String orderId) {
        log.info("Received request to cancel reservation for Order ID: {}", orderId);
        ReservationResponse response = reservationService.cancelReservation(orderId);
        return ResponseEntity.ok(
                ApiResponse.<ReservationResponse>builder()
                        .success(true)
                        .data(response)
                        .message("Reservation cancelled successfully")
                        .timestamp(Instant.now())
                        .build()
        );
    }
}