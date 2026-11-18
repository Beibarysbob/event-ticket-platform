package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.catalog.api.CatalogDtos;
import kz.kimep.ticketing.messaging.OutboxService;
import kz.kimep.ticketing.shared.domain.Booking;
import kz.kimep.ticketing.shared.domain.Seat;
import kz.kimep.ticketing.shared.domain.SeatHold;
import kz.kimep.ticketing.shared.domain.Ticket;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.BookingRepository;
import kz.kimep.ticketing.shared.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.shared.repo.SeatHoldRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookings;
    private final SeatHoldRepository holds;
    private final TicketRepository tickets;
    private final EventSectionPriceRepository prices;
    private final HoldService holdService;
    private final OutboxService outbox;
    private final BookingRules rules;
    private final PricingRules pricing;

    @Transactional
    public BookingDtos.BookingView create(String holdReference, UserAccount user) {
        SeatHold hold = holds.findLockedByReference(holdReference).orElseThrow(() ->
            new DomainException(HttpStatus.NOT_FOUND, "Hold was not found"));
        requireUsable(hold, user.getId());
        Booking booking = new Booking();
        booking.setBookingReference(UUID.randomUUID().toString());
        booking.setEvent(hold.getEvent());
        booking.setUser(user);
        booking.setStatus("PENDING");
        booking.setCurrency("KZT");
        booking.setCreatedAt(Instant.now());
        bookings.saveAndFlush(booking);
        BigDecimal subtotal = issueReserved(booking, hold);
        booking.setSubtotal(subtotal);
        booking.setFees(BigDecimal.ZERO);
        booking.setTotal(subtotal);
        hold.setStatus("CONSUMED");
        List<Long> ids = hold.getSeats().stream().map(Seat::getId).toList();
        holdService.afterCommitRelease(hold.getEvent().getId(), ids, holdReference);
        return view(booking);
    }

    @Transactional(readOnly = true)
    public BookingDtos.BookingView get(String reference, Long userId) {
        return view(requireOwned(reference, userId));
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageView<BookingDtos.BookingView> list(Long userId, int page, int size) {
        int limited = Math.min(size, 50);
        var result = bookings.findByUserId(userId, PageRequest.of(page, limited));
        return new CatalogDtos.PageView<>(result.map(this::view).getContent(),
            page, limited, result.getTotalElements());
    }

    @Transactional
    public void cancel(String reference, Long userId) {
        Booking booking = requireOwned(reference, userId);
        rules.requirePending(booking.getStatus());
        booking.setStatus("CANCELLED");
        booking.setCancelledAt(Instant.now());
        tickets.deleteAll(tickets.findByBookingId(booking.getId()));
        outbox.record(booking, "booking.cancelled");
    }

    @Transactional(readOnly = true)
    public Booking requireOwned(String reference, Long userId) {
        Booking booking = bookings.findByBookingReference(reference).orElseThrow(() ->
            new DomainException(HttpStatus.NOT_FOUND, "Booking was not found"));
        if (!booking.getUser().getId().equals(userId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Booking was not found");
        }
        return booking;
    }

    private void requireUsable(SeatHold hold, Long userId) {
        if (!hold.getUser().getId().equals(userId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Hold was not found");
        }
        rules.requireLive(hold.getStatus(), hold.getExpiresAt(), Instant.now());
    }

    private BigDecimal issueReserved(Booking booking, SeatHold hold) {
        java.util.List<BigDecimal> amounts = new java.util.ArrayList<>();
        java.util.Map<Long, BigDecimal> priceBySection = new java.util.HashMap<>();
        prices.findByEventId(hold.getEvent().getId()).forEach(price ->
            priceBySection.put(price.getSection().getId(), price.getPrice()));
        for (Seat seat : hold.getSeats()) {
            BigDecimal price = priceBySection.get(seat.getSection().getId());
            if (price == null) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "Ticket price not configured");
            }
            Ticket ticket = new Ticket();
            ticket.setTicketCode(UUID.randomUUID().toString());
            ticket.setEvent(hold.getEvent());
            ticket.setBooking(booking);
            ticket.setSeat(seat);
            ticket.setPricePaid(price);
            ticket.setStatus("RESERVED");
            tickets.save(ticket);
            amounts.add(price);
        }
        tickets.flush();
        return pricing.subtotal(amounts);
    }

    private BookingDtos.BookingView view(Booking booking) {
        List<String> codes = tickets.findByBookingId(booking.getId()).stream()
            .map(Ticket::getTicketCode).toList();
        return new BookingDtos.BookingView(booking.getBookingReference(), booking.getEvent().getId(),
            booking.getStatus(), booking.getTotal(), codes);
    }
}
