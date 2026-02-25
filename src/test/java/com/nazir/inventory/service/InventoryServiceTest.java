package com.nazir.inventory.service;

import com.nazir.inventory.dto.ReservationRequest;
import com.nazir.inventory.dto.ReservationResponse;
import com.nazir.inventory.entity.Product;
import com.nazir.inventory.entity.Reservation;
import com.nazir.inventory.entity.ReservationStatus;
import com.nazir.inventory.exception.DuplicateOrderException;
import com.nazir.inventory.exception.OutOfStockException;
import com.nazir.inventory.repository.ProductRepository;
import com.nazir.inventory.repository.ReservationRepository;
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
}
