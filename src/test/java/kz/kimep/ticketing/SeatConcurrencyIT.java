package kz.kimep.ticketing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.booking.service.HoldService;
import kz.kimep.ticketing.booking.service.SeatLockService;
import kz.kimep.ticketing.shared.domain.Event;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.repo.EventRepository;
import kz.kimep.ticketing.shared.repo.SeatRepository;
import kz.kimep.ticketing.shared.repo.SectionRepository;
import kz.kimep.ticketing.shared.repo.UserAccountRepository;
import kz.kimep.ticketing.shared.repo.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SeatConcurrencyIT extends IntegrationContainers {
    @Autowired private HoldService holds;
    @Autowired private SeatLockService locks;
    @Autowired private EventRepository events;
    @Autowired private SeatRepository seats;
    @Autowired private SectionRepository sections;
    @Autowired private VenueRepository venues;
    @Autowired private UserAccountRepository users;

    @Test
    void twoBuyersOneSeatExactlyOneHoldSucceeds() throws Exception {
        Event event = setupEvent();
        Long seatId = firstSeat(event);
        var first = makeUser();
        var second = makeUser();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> buyOne = () -> attemptHold(event.getId(), seatId, first, start);
        Callable<Boolean> buyTwo = () -> attemptHold(event.getId(), seatId, second, start);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(buyOne);
            var b = executor.submit(buyTwo);
            start.countDown();
            assertEquals(1, (a.get(10, TimeUnit.SECONDS) ? 1 : 0)
                + (b.get(10, TimeUnit.SECONDS) ? 1 : 0));
        }
    }

    @Test
    void partialMultiSeatHoldFailureReleasesEarlierSeat() {
        Event event = setupEvent();
        var section = sections.findByVenueId(event.getVenue().getId()).get(0);
        var seatPage = seats.findBySectionId(section.getId(), PageRequest.of(0, 2));
        Long firstSeat = seatPage.getContent().get(0).getId();
        Long secondSeat = seatPage.getContent().get(1).getId();
        String reference = UUID.randomUUID().toString();
        assertTrue(locks.claim(event.getId(), secondSeat, reference, java.time.Duration.ofMinutes(10)));
        try {
            assertThrows(kz.kimep.ticketing.shared.error.DomainException.class, () ->
                holds.hold(new BookingDtos.HoldInput(event.getId(), List.of(firstSeat, secondSeat)), makeUser()));
            assertFalse(locks.isHeld(event.getId(), firstSeat));
            assertTrue(locks.isHeld(event.getId(), secondSeat));
        } finally {
            locks.release(event.getId(), List.of(secondSeat), reference);
        }
    }

    private boolean attemptHold(Long eventId, Long seatId, UserAccount user, CountDownLatch start) throws Exception {
        start.await();
        try {
            holds.hold(new BookingDtos.HoldInput(eventId, List.of(seatId)), user);
            return true;
        } catch (kz.kimep.ticketing.shared.error.DomainException error) {
            assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatus());
            return false;
        }
    }

    private Event setupEvent() {
        Event event = new Event();
        event.setVenue(venues.findById(1L).orElseThrow());
        event.setOrganizer(makeUser());
        event.setTitle("Concurrent Test " + UUID.randomUUID());
        event.setStartsAt(Instant.now().plusSeconds(86400));
        event.setSalesOpenAt(Instant.now().minusSeconds(60));
        event.setSalesCloseAt(Instant.now().plusSeconds(3600));
        event.setStatus("ON_SALE");
        return events.save(event);
    }

    private Long firstSeat(Event event) {
        var section = sections.findByVenueId(event.getVenue().getId()).get(0);
        return seats.findBySectionId(section.getId(), PageRequest.of(0, 1))
            .getContent().get(0).getId();
    }

    private UserAccount makeUser() {
        UserAccount user = new UserAccount();
        user.setEmail(UUID.randomUUID() + "@example.org");
        user.setPasswordHash("test-only");
        user.setRole("CUSTOMER");
        user.setEnabled(true);
        user.setDisplayName("Test");
        return users.save(user);
    }
}
