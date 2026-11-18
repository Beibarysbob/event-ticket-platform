package kz.kimep.ticketing.booking;

import java.math.BigDecimal;
import java.util.List;
import kz.kimep.ticketing.booking.service.PricingRules;
import kz.kimep.ticketing.shared.error.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PricingRulesTest {
    private final PricingRules rules = new PricingRules();
    @Test
    void oneTicketTotal() {
        assertEquals(0, rules.subtotal(List.of(new BigDecimal("2000"))).compareTo(new BigDecimal("2000")));
    }

    @Test
    void twoTicketsTotal() {
        assertEquals(0, rules.subtotal(List.of(new BigDecimal("2000"),new BigDecimal("3000"))).compareTo(new BigDecimal("5000")));
    }

    @Test
    void decimalPricesDoNotRound() {
        assertEquals(0, rules.subtotal(List.of(new BigDecimal("0.10"),new BigDecimal("0.20"))).compareTo(new BigDecimal("0.30")));
    }

    @Test
    void largePriceIsPreserved() {
        assertEquals(0, rules.subtotal(List.of(new BigDecimal("999999.99"))).compareTo(new BigDecimal("999999.99")));
    }

    @Test
    void positiveTinyPriceAccepted() {
        assertEquals(0, rules.subtotal(List.of(new BigDecimal("0.01"))).compareTo(new BigDecimal("0.01")));
    }

    @Test
    void emptyPricesRejected() {
        assertThrows(DomainException.class, () -> rules.subtotal(List.of()));
    }

    @Test
    void nullPricesRejected() {
        assertThrows(DomainException.class, () -> rules.subtotal(null));
    }

    @Test
    void zeroPriceRejected() {
        assertThrows(DomainException.class, () -> rules.subtotal(List.of(BigDecimal.ZERO)));
    }

    @Test
    void negativePriceRejected() {
        assertThrows(DomainException.class, () -> rules.subtotal(List.of(new BigDecimal("-1"))));
    }

    @Test
    void nullItemRejected() {
        assertThrows(DomainException.class, () -> rules.subtotal(java.util.Arrays.asList(BigDecimal.ONE, null)));
    }

    @Test
    void rejectsNegativeAmongPositive() {
        assertThrows(DomainException.class, () -> rules.subtotal(List.of(BigDecimal.ONE, BigDecimal.ONE.negate())));
    }

    @Test
    void rejectsZeroAfterPositive() {
        assertThrows(DomainException.class, () -> rules.subtotal(List.of(BigDecimal.ONE, BigDecimal.ZERO)));
    }

}
