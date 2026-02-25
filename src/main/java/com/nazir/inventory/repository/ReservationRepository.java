package com.nazir.inventory.repository;

import com.nazir.inventory.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByOrderId(String orderId);
    Optional<Reservation> findByOrderId(String orderId);
}
