package kz.kimep.ticketing.booking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.catalog.service.EventService;
import kz.kimep.ticketing.shared.domain.Event;
import kz.kimep.ticketing.shared.domain.Seat;
import kz.kimep.ticketing.shared.domain.SeatHold;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.SeatHoldRepository;
import kz.kimep.ticketing.shared.repo.SeatRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class HoldService {
    private final EventService events;
    private final SeatRepository seats;
    private final SeatHoldRepository holds;
    private final TicketRepository tickets;
    private final SeatLockService locks;
    private final BookingRules rules;
    @Value("${ticketing.hold-ttl}")
    private Duration holdTtl;
    @Value("${ticketing.max-seats-per-booking}")
    private int maxSeats;

    @Transactional
    public BookingDtos.HoldView hold(BookingDtos.HoldInput input, UserAccount user) {
        Event event = events.require(input.eventId());
        requireValid(event, input.seatIds());
        String reference = UUID.randomUUID().toString();
        List<Long> claimed = new ArrayList<>();
        try {
            for (Long seatId : input.seatIds()) {
                if (!locks.claim(event.getId(), seatId, reference, holdTtl)) {
                    throw new DomainException(HttpStatus.CONFLICT, "One or more seats are already held");
                }
                claimed.add(seatId);
            }
            SeatHold hold = persist(event, user, reference, input.seatIds());
            rollbackReleases(event.getId(), claimed, reference);
            return view(hold);
        } catch (RuntimeException error) {
            locks.release(event.getId(), claimed, reference);
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public BookingDtos.HoldView get(String reference, Long userId) {
        return view(requireOwned(reference, userId));
    }

    @Transactional
    public void release(String reference, Long userId) {
        SeatHold hold = requireOwned(reference, userId);
        if ("HELD".equals(hold.getStatus())) {
            hold.setStatus("RELEASED");
            var ids = hold.getSeats().stream().map(Seat::getId).toList();
            afterCommitRelease(hold.getEvent().getId(), ids, reference);
        }
    }

    @Transactional(readOnly = true)
    public SeatHold requireOwned(String ref, Long userId) {
        SeatHold hold = holds.findByHoldReference(ref).orElseThrow(() ->
            new DomainException(HttpStatus.NOT_FOUND, "Hold was not found"));
        if (!hold.getUser().getId().equals(userId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Hold was not found");
        }
        return hold;
    }

    private void requireValid(Event event, List<Long> ids) {
        rules.requireDistinctSeats(ids, maxSeats);
        if (!rules.isSaleOpen(event.getStatus(), event.getSalesOpenAt(),
                event.getSalesCloseAt(), Instant.now())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Event is not currently on sale");
        }
        List<Seat> found = seats.findByIdIn(ids);
        if (found.size() != ids.size() || found.stream().anyMatch(seat ->
                !seat.getSection().getVenue().getId().equals(event.getVenue().getId()))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "seatIds must belong to the event venue");
        }
        if (ids.stream().anyMatch(new java.util.HashSet<>(tickets.findSoldSeatIds(event.getId()))::contains)) {
            throw new DomainException(HttpStatus.CONFLICT, "A seat has already been booked");
        }
    }

    private SeatHold persist(Event event, UserAccount user, String ref, List<Long> seatIds) {
        SeatHold hold = new SeatHold();
        hold.setHoldReference(ref);
        hold.setEvent(event);
        hold.setUser(user);
        hold.setStatus("HELD");
        hold.setCreatedAt(Instant.now());
        hold.setExpiresAt(Instant.now().plus(holdTtl));
        hold.setSeats(new HashSet<>(seats.findByIdIn(seatIds)));
        return holds.save(hold);
    }

    public BookingDtos.HoldView view(SeatHold hold) {
        long remaining = Math.max(0, Duration.between(Instant.now(), hold.getExpiresAt()).toSeconds());
        String status = remaining == 0 && "HELD".equals(hold.getStatus()) ? "EXPIRED" : hold.getStatus();
        return new BookingDtos.HoldView(hold.getHoldReference(), status, hold.getExpiresAt(), remaining);
    }

    public void afterCommitRelease(Long eventId, List<Long> ids, String ref) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                locks.release(eventId, ids, ref);
            }
        });
    }

    private void rollbackReleases(Long eventId, List<Long> ids, String ref) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    locks.release(eventId, ids, ref);
                }
            }
        });
    }
}
