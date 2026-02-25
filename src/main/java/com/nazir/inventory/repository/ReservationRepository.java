package com.nazir.inventory.repository;

import com.nazir.inventory.entity.Reservation;
import com.nazir.inventory.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByOrderId(String orderId);
    Optional<Reservation> findByOrderId(String orderId);
    List<Reservation> findAllByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
