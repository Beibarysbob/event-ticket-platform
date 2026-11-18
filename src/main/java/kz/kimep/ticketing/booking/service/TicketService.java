package kz.kimep.ticketing.booking.service;

import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.catalog.service.EventService;
import kz.kimep.ticketing.shared.domain.Ticket;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository tickets;
    private final EventService events;

    @Transactional(readOnly = true)
    public BookingDtos.TicketView get(String code, Long userId) {
        Ticket ticket = tickets.findByTicketCode(code).orElseThrow(() ->
            new DomainException(HttpStatus.NOT_FOUND, "Ticket was not found"));
        var event = events.require(ticket.getEvent().getId());
        boolean owner = ticket.getBooking().getUser().getId().equals(userId);
        boolean organizer = event.getOrganizer().getId().equals(userId);
        if (!owner && !organizer) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Ticket was not found");
        }
        return new BookingDtos.TicketView(ticket.getTicketCode(), event.getId(),
            ticket.getSeat().getId(), ticket.getStatus(), ticket.getPricePaid());
    }
}
