package kz.kimep.ticketing.booking.service;

import java.time.Duration;
import java.time.Instant;
import kz.kimep.ticketing.shared.domain.Seat;
import kz.kimep.ticketing.shared.repo.SeatHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HoldReconciliation {
    private final SeatHoldRepository holds;
    private final SeatLockService locks;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void restoreAfterRedisRestart() {
        holds.findByStatusAndExpiresAtAfter("HELD", Instant.now()).forEach(hold -> {
            Duration remaining = Duration.between(Instant.now(), hold.getExpiresAt());
            for (Seat seat : hold.getSeats()) {
                locks.claim(hold.getEvent().getId(), seat.getId(), hold.getHoldReference(), remaining);
            }
        });
    }
}
