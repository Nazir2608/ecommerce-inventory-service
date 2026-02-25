package com.nazir.inventory.service;

import com.nazir.inventory.dto.ReservationRequest;
import com.nazir.inventory.dto.ReservationResponse;
import com.nazir.inventory.entity.Reservation;
import com.nazir.inventory.entity.ReservationStatus;
import com.nazir.inventory.exception.DuplicateOrderException;
import com.nazir.inventory.exception.InvalidStateException;
import com.nazir.inventory.exception.OutOfStockException;
import com.nazir.inventory.repository.ProductRepository;
import com.nazir.inventory.repository.ReservationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private ReservationRequest reservationRequest;

    @BeforeEach
    void setUp() {
        reservationRequest = ReservationRequest.builder()
                .productId(1L)
                .orderId("ORDER-123")
                .quantity(5)
                .build();
    }

    @Test
    void cancelReservation_ShouldRestoreStock_WhenReserved() {
        // Arrange
        Reservation reservation = Reservation.builder()
                .orderId("ORDER-123")
                .productId(1L)
                .quantity(5)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(reservation));
        when(productRepository.restoreStock(1L, 5)).thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        ReservationResponse response = inventoryService.cancelReservation("ORDER-123");

        // Assert
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(productRepository).restoreStock(1L, 5);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelReservation_ShouldRestock_WhenConfirmed() {
        // Arrange
        Reservation reservation = Reservation.builder()
                .orderId("ORDER-123")
                .productId(1L)
                .quantity(5)
                .status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(reservation));
        when(productRepository.restockProduct(1L, 5)).thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        ReservationResponse response = inventoryService.cancelReservation("ORDER-123");

        // Assert
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(productRepository).restockProduct(1L, 5);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelReservation_ShouldThrowInvalidStateException_WhenAlreadyCancelled() {
        // Arrange
        Reservation reservation = Reservation.builder()
                .orderId("ORDER-123")
                .status(ReservationStatus.CANCELLED)
                .build();
        when(reservationRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.cancelReservation("ORDER-123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Reservation must be in RESERVED or CONFIRMED state");
        
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveInventory_ShouldReserveStock_WhenStockAvailable() {
        // Arrange
        when(reservationRepository.existsByOrderId("ORDER-123")).thenReturn(false);
        when(productRepository.reserveStock(1L, 5)).thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        // Act
        ReservationResponse response = inventoryService.reserveInventory(reservationRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("ORDER-123");
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        verify(productRepository).reserveStock(1L, 5);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveInventory_ShouldThrowDuplicateOrderException_WhenOrderIdExists() {
        // Arrange
        when(reservationRepository.existsByOrderId("ORDER-123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserveInventory(reservationRequest))
                .isInstanceOf(DuplicateOrderException.class)
                .hasMessageContaining("Order ID already exists");
        
        verify(productRepository, never()).reserveStock(anyLong(), anyInt());
    }

    @Test
    void reserveInventory_ShouldThrowOutOfStockException_WhenStockInsufficient() {
        // Arrange
        when(reservationRepository.existsByOrderId("ORDER-123")).thenReturn(false);
        when(productRepository.reserveStock(1L, 5)).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserveInventory(reservationRequest))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("Insufficient stock");
        
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void confirmReservation_ShouldDeductStockAndConfirm_WhenReserved() {
        // Arrange
        Reservation reservation = Reservation.builder()
                .orderId("ORDER-123")
                .productId(1L)
                .quantity(5)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(reservation));
        when(productRepository.confirmStock(1L, 5)).thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        ReservationResponse response = inventoryService.confirmReservation("ORDER-123");

        // Assert
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(productRepository).confirmStock(1L, 5);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void confirmReservation_ShouldThrowInvalidStateException_WhenStockUpdateFails() {
        // Arrange
        Reservation reservation = Reservation.builder()
                .orderId("ORDER-123")
                .productId(1L)
                .quantity(5)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(reservation));
        when(productRepository.confirmStock(1L, 5)).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.confirmReservation("ORDER-123"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Failed to confirm stock");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_ShouldRestoreStock_WhenReserved() {
        // Arrange
        Reservation reservation = Reservation.builder()
                .orderId("ORDER-123")
                .productId(1L)
                .quantity(5)
                .status(ReservationStatus.RESERVED)
                .build();
        when(reservationRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(reservation));
        when(productRepository.restoreStock(1L, 5)).thenReturn(1);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // Act
        ReservationResponse response = inventoryService.releaseReservation("ORDER-123");

        // Assert
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(productRepository).restoreStock(1L, 5);
        verify(reservationRepository).save(reservation);
    }
}
