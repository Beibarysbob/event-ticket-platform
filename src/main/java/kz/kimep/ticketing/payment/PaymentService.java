package kz.kimep.ticketing.payment;

import java.time.Instant;
import java.util.UUID;
import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.messaging.OutboxService;
import kz.kimep.ticketing.shared.domain.Booking;
import kz.kimep.ticketing.shared.domain.Payment;
import kz.kimep.ticketing.shared.domain.Ticket;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.BookingRepository;
import kz.kimep.ticketing.shared.repo.PaymentRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final TicketRepository tickets;
    private final OutboxService outbox;
    private final kz.kimep.ticketing.booking.service.BookingRules rules;

    @Transactional
    public BookingDtos.BookingView pay(BookingDtos.PaymentInput input, Long userId) {
        Booking booking = bookings.findByBookingReference(input.bookingReference())
            .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Booking was not found"));
        if (!booking.getUser().getId().equals(userId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Booking was not found");
        }
        rules.requirePending(booking.getStatus());
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setMethod("SIMULATED");
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setAmount(booking.getTotal());
        payment.setCurrency(booking.getCurrency());
        payment.setStatus(input.approve() ? "PAID" : "DECLINED");
        payments.save(payment);
        if (!input.approve()) {
            decline(booking, payment);
        } else {
            confirm(booking, payment);
        }
        return view(booking);
    }

    private void decline(Booking booking, Payment payment) {
        payment.setFailureCode("SIMULATED_DECLINE");
        booking.setStatus("CANCELLED");
        booking.setCancelledAt(Instant.now());
        tickets.deleteAll(tickets.findByBookingId(booking.getId()));
        outbox.record(booking, "payment.failed");
        // 402 is returned after the transaction commits via an explicit result in the controller.
    }

    private void confirm(Booking booking, Payment payment) {
        payment.setPaidAt(Instant.now());
        booking.setStatus("CONFIRMED");
        booking.setConfirmedAt(Instant.now());
        outbox.record(booking, "booking.confirmed");
    }

    private BookingDtos.BookingView view(Booking booking) {
        return new BookingDtos.BookingView(booking.getBookingReference(), booking.getEvent().getId(),
            booking.getStatus(), booking.getTotal(), tickets.findByBookingId(booking.getId())
                .stream().map(Ticket::getTicketCode).toList());
    }
}
