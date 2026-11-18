package kz.kimep.ticketing.booking;

import java.time.Instant;
import java.util.List;
import kz.kimep.ticketing.booking.service.BookingRules;
import kz.kimep.ticketing.shared.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class BookingRulesTest {
    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private final BookingRules rules = new BookingRules();
    @Test
    void oneSeatIsAllowed() {
        assertDoesNotThrow(() -> rules.requireDistinctSeats(List.of(1L), 8));
    }

    @Test
    void eightSeatsAllowed() {
        assertDoesNotThrow(() -> rules.requireDistinctSeats(List.of(1L,2L,3L,4L,5L,6L,7L,8L), 8));
    }

    @Test
    void nineSeatsRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(DomainException.class, () -> rules.requireDistinctSeats(List.of(1L,2L,3L,4L,5L,6L,7L,8L,9L), 8)).getStatus());
    }

    @Test
    void duplicateSeatsRejected() {
        assertThrows(DomainException.class, () -> rules.requireDistinctSeats(List.of(1L,1L), 8));
    }

    @Test
    void emptySeatListRejected() {
        assertThrows(DomainException.class, () -> rules.requireDistinctSeats(List.of(), 8));
    }

    @Test
    void nullSeatListRejected() {
        assertThrows(DomainException.class, () -> rules.requireDistinctSeats(null, 8));
    }

    @Test
    void nullSeatItemRejected() {
        assertThrows(DomainException.class, () -> rules.requireDistinctSeats(java.util.Arrays.asList(1L, null), 8));
    }

    @Test
    void zeroSeatLimitRejected() {
        assertThrows(DomainException.class, () -> rules.requireDistinctSeats(List.of(1L), 0));
    }

    @Test
    void releasedHoldRejected() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(DomainException.class, () -> rules.requireLive("RELEASED", Instant.now().plusSeconds(60), Instant.now())).getStatus());
    }

    @Test
    void consumedHoldRejected() {
        assertThrows(DomainException.class, () -> rules.requireLive("CONSUMED", Instant.now().plusSeconds(60), Instant.now()));
    }

    @Test
    void expiredHoldRejected() {
        assertEquals(HttpStatus.GONE, assertThrows(DomainException.class, () -> rules.requireLive("HELD", Instant.now().minusSeconds(1), Instant.now())).getStatus());
    }

    @Test
    void holdExactlyAtExpiryRejected() {
        assertThrows(DomainException.class, () -> rules.requireLive("HELD", NOW, NOW));
    }

    @Test
    void holdBeforeExpiryAllowed() {
        assertDoesNotThrow(() -> rules.requireLive("HELD", NOW.plusSeconds(1), NOW));
    }

    @Test
    void pendingBookingAllowed() {
        assertDoesNotThrow(() -> rules.requirePending("PENDING"));
    }

    @Test
    void confirmedBookingCannotBeRepaid() {
        assertThrows(DomainException.class, () -> rules.requirePending("CONFIRMED"));
    }

    @Test
    void cancelledBookingCannotBeRepaid() {
        assertThrows(DomainException.class, () -> rules.requirePending("CANCELLED"));
    }

    @Test
    void saleOpensAtExactInstant() {
        assertTrue(rules.isSaleOpen("ON_SALE", NOW, NOW.plusSeconds(60), NOW));
    }

    @Test
    void saleClosesAtExactInstant() {
        assertFalse(rules.isSaleOpen("ON_SALE", NOW.minusSeconds(60), NOW, NOW));
    }

    @Test
    void saleNotOpenBeforeStart() {
        assertFalse(rules.isSaleOpen("ON_SALE", NOW.plusSeconds(1), NOW.plusSeconds(100), NOW));
    }

    @Test
    void draftEventCannotSell() {
        assertFalse(rules.isSaleOpen("DRAFT", NOW.minusSeconds(1), NOW.plusSeconds(100), NOW));
    }

    @Test
    void soldOutEventCannotSell() {
        assertFalse(rules.isSaleOpen("SOLD_OUT", NOW.minusSeconds(1), NOW.plusSeconds(100), NOW));
    }

    @Test
    void missingSalesOpenRejected() {
        assertFalse(rules.isSaleOpen("ON_SALE", null, NOW.plusSeconds(1), NOW));
    }

    @Test
    void missingSalesCloseRejected() {
        assertFalse(rules.isSaleOpen("ON_SALE", NOW.minusSeconds(1), null, NOW));
    }

    @Test
    void unknownStatusCannotSell() {
        assertFalse(rules.isSaleOpen("UNKNOWN", NOW.minusSeconds(1), NOW.plusSeconds(1), NOW));
    }

    @Test
    void nullHoldExpiryRejected() {
        assertThrows(DomainException.class, () -> rules.requireLive("HELD", null, NOW));
    }

}
