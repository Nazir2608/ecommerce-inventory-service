package com.nazir.inventory.service;

import com.nazir.inventory.dto.ProductRequest;
import com.nazir.inventory.dto.ProductResponse;
import com.nazir.inventory.dto.ReservationRequest;
import com.nazir.inventory.dto.ReservationResponse;
import com.nazir.inventory.entity.Product;
import com.nazir.inventory.entity.Reservation;
import com.nazir.inventory.entity.ReservationStatus;
import com.nazir.inventory.exception.DuplicateOrderException;
import com.nazir.inventory.exception.OutOfStockException;
import com.nazir.inventory.exception.ResourceNotFoundException;
import com.nazir.inventory.repository.ProductRepository;
import com.nazir.inventory.repository.ReservationRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private static final long RESERVATION_EXPIRY_MINUTES = 1;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product with name: {}", request.getName());
        Product product = Product.builder()
                .name(request.getName())
                .totalStock(request.getTotalStock())
                .reservedStock(0)
                .build();
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with id: " + id);
                });
        return mapToProductResponse(product);
    }

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
                .expiresAt(Instant.now().plus(Duration.ofMinutes(RESERVATION_EXPIRY_MINUTES)))
                .build();
        
        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reservation successful for Order ID: {}, Reservation ID: {}, Expires at: {}", 
                request.getOrderId(), savedReservation.getId(), savedReservation.getExpiresAt());

        return mapToReservationResponse(savedReservation);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .totalStock(product.getTotalStock())
                .reservedStock(product.getReservedStock())
                .availableStock(product.getTotalStock() - product.getReservedStock())
                .build();
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
}
