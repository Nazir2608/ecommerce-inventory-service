package com.nazir.inventory.scheduler;

import com.nazir.inventory.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryScheduler {

    private final ReservationService reservationService;

    @Scheduled(fixedRateString = "${inventory.scheduler.expiry-rate}")
    @Transactional
    public void runExpiryJob() {
        log.debug("Triggering scheduled reservation expiry check...");
        reservationService.expireReservations();
    }
}