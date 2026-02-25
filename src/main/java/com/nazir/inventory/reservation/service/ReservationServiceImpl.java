package com.nazir.inventory.reservation.service;

import com.nazir.inventory.exception.DuplicateOrderException;
import com.nazir.inventory.exception.InvalidStateException;
import com.nazir.inventory.exception.OutOfStockException;
import com.nazir.inventory.exception.ResourceNotFoundException;
import com.nazir.inventory.product.repository.ProductRepository;
import com.nazir.inventory.event.InventoryEvent;
import com.nazir.inventory.kafka.producer.InventoryEventProducer;
import com.nazir.inventory.reservation.dto.ReservationRequest;
import com.nazir.inventory.reservation.dto.ReservationResponse;
import com.nazir.inventory.reservation.entity.Reservation;
import com.nazir.inventory.reservation.entity.ReservationStatus;
import com.nazir.inventory.reservation.event.ReservationStockRestoredEvent;
import com.nazir.inventory.reservation.repository.ReservationRepository;
import com.nazir.inventory.reservation.service.expiration.ReservationExpirationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryEventProducer inventoryEventProducer;
    private final ReservationExpirationService reservationExpirationService;

    @Value("${inventory.reservation.expiry-minutes:15}")
    private long reservationExpiryMinutes;

    @Override
    @Transactional
    public ReservationResponse reserveInventory(ReservationRequest request) {
        log.info("Attempting to reserve inventory for Order ID: {}, Product ID: {}, Quantity: {}", request.getOrderId(), request.getProductId(), request.getQuantity());
        // 1. Check duplicate orderId
        if (reservationRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Duplicate reservation attempt for Order ID: {}", request.getOrderId());
            throw new DuplicateOrderException("Order ID already exists: " + request.getOrderId());
        }
        // 2. Execute atomic stock update
        int rowsAffected = productRepository.reserveStock(request.getProductId(), request.getQuantity());
        if (rowsAffected == 0) {
            log.error("Failed to reserve stock. Insufficient stock or invalid product ID: {}", request.getProductId());
            throw new OutOfStockException("Insufficient stock or invalid product for ID: " + request.getProductId());
        }
        // 3. Insert reservation record
        Reservation reservation = Reservation.builder()
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .status(ReservationStatus.RESERVED)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(reservationExpiryMinutes)))
                .build();
        
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation successful for Order ID: {}, Reservation ID: {}, Expires at: {}", request.getOrderId(), savedReservation.getId(), savedReservation.getExpiresAt());
        
        publishInventoryEvent(savedReservation, "RESERVED");
        
        return mapToReservationResponse(savedReservation);
    }

    @Override
    @Transactional
    public ReservationResponse confirmReservation(String orderId) {
        log.info("Confirming reservation for Order ID: {}", orderId);
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for orderId: " + orderId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.warn("Reservation already confirmed for Order ID: {}", orderId);
            return mapToReservationResponse(reservation);
        }

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Cannot confirm reservation in status: {} for Order ID: {}", reservation.getStatus(), orderId);
            throw new InvalidStateException("Reservation must be in RESERVED state to confirm");
        }
        // Permanently deduct stock from both total and reserved
        int rowsAffected = productRepository.confirmStock(reservation.getProductId(), reservation.getQuantity());
        if (rowsAffected == 0) {
            log.error("Failed to confirm stock. Inconsistent reserved stock for product ID: {}", reservation.getProductId());
            throw new InvalidStateException("Failed to confirm stock: Inconsistent reserved stock for product ID: " + reservation.getProductId());
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation confirmed and stock deducted for Order ID: {}", orderId);
        publishInventoryEvent(savedReservation, "CONFIRMED");
        return mapToReservationResponse(savedReservation);
    }

    @Override
    @Transactional
    public ReservationResponse releaseReservation(String orderId) {
        log.info("Releasing reservation for Order ID: {}", orderId);
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for orderId: " + orderId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            log.warn("Cannot release reservation in status: {} for Order ID: {}", reservation.getStatus(), orderId);
            throw new InvalidStateException("Reservation must be in RESERVED state to release");
        }
        // Restore stock
        int rowsAffected = productRepository.restoreStock(reservation.getProductId(), reservation.getQuantity());
        if (rowsAffected == 0) {
            log.error("Failed to restore stock for product: {}", reservation.getProductId());
        } else {
            eventPublisher.publishEvent(new ReservationStockRestoredEvent(reservation.getProductId(), reservation.getQuantity()));
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation released and stock restored for Order ID: {}", orderId);
        publishInventoryEvent(savedReservation, "RELEASED");
        return mapToReservationResponse(savedReservation);
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservation(String orderId) {
        log.info("Cancelling reservation for Order ID: {}", orderId);
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found for orderId: " + orderId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            // Case 1: Post-confirmation cancellation (Restocking)
            log.info("Restocking confirmed reservation for Order ID: {}", orderId);
            productRepository.restockProduct(reservation.getProductId(), reservation.getQuantity());
            eventPublisher.publishEvent(new ReservationStockRestoredEvent(reservation.getProductId(), reservation.getQuantity()));
        } else if (reservation.getStatus() == ReservationStatus.RESERVED) {
            // Case 2: Pre-confirmation cancellation (Releasing stock)
            log.info("Releasing reserved stock for Order ID: {}", orderId);
            productRepository.restoreStock(reservation.getProductId(), reservation.getQuantity());
            eventPublisher.publishEvent(new ReservationStockRestoredEvent(reservation.getProductId(), reservation.getQuantity()));
        } else if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.RELEASED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            log.warn("Reservation already in terminal state: {} for Order ID: {}", reservation.getStatus(), orderId);
            return mapToReservationResponse(reservation);
        } else {
            // Case 3: Unknown/Invalid state
            log.warn("Cannot cancel reservation in status: {} for Order ID: {}", reservation.getStatus(), orderId);
            throw new InvalidStateException("Reservation must be in RESERVED or CONFIRMED state to cancel");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation cancelled for Order ID: {}", orderId);
        publishInventoryEvent(savedReservation, "CANCELLED");
        return mapToReservationResponse(savedReservation);
    }

    @Override
    public void expireReservations() {
        log.debug("Running expiry scheduler...");
        List<Reservation> expiredReservations = reservationRepository.findAllByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, Instant.now());
        if (expiredReservations.isEmpty()) {
            return;
        }
        log.info("Found {} expired reservations to process", expiredReservations.size());
        for (Reservation reservation : expiredReservations) {
            try {
               reservationExpirationService.processExpiration(reservation);
            } catch (Exception e) {
                log.error("Error expiring reservation for Order ID: {}", reservation.getOrderId(), e);
            }
        }
    }

    @Override
    public void processExpiration(Reservation reservation) {
        reservationExpirationService.processExpiration(reservation);
    }

    private ReservationResponse mapToReservationResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .build();
    }

    private void publishInventoryEvent(Reservation reservation, String eventType) {
        InventoryEvent event = InventoryEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(eventType)
                .orderId(reservation.getOrderId())
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .timestamp(Instant.now())
                .build();
        inventoryEventProducer.publishEvent(event);
    }
}