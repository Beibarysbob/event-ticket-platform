package kz.kimep.ticketing.booking.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class BookingDtos {
    private BookingDtos() { }
    public record HoldInput(@NotNull Long eventId, @NotEmpty List<@NotNull Long> seatIds) { }
    public record HoldView(String reference, String status, Instant expiresAt,
                           long remainingSeconds) { }
    public record BookingInput(@NotBlank String holdReference) { }
    public record BookingView(String reference, Long eventId, String status,
                              BigDecimal total, List<String> ticketCodes) { }
    public record PaymentInput(@NotBlank String bookingReference, boolean approve) { }
    public record TicketView(String code, Long eventId, Long seatId,
                             String status, BigDecimal paid) { }
    public record SalesView(Long eventId, long confirmedBookings,
                            long issuedTickets, BigDecimal revenue) { }
}
