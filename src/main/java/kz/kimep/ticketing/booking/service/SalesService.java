package kz.kimep.ticketing.booking.service;

import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.catalog.service.EventService;
import kz.kimep.ticketing.shared.repo.BookingRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesService {
    private final EventService events;
    private final BookingRepository bookings;
    private final TicketRepository tickets;

    @Transactional(readOnly = true)
    public BookingDtos.SalesView summary(Long eventId, Long userId) {
        events.requireOwned(eventId, userId);
        return new BookingDtos.SalesView(eventId,
            bookings.countByEventIdAndStatus(eventId, "CONFIRMED"),
            tickets.countByEventIdAndStatus(eventId, "ISSUED"),
            bookings.totalSales(eventId));
    }
}
