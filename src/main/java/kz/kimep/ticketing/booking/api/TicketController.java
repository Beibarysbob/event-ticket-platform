package kz.kimep.ticketing.booking.api;

import kz.kimep.ticketing.booking.service.SalesService;
import kz.kimep.ticketing.booking.service.TicketService;
import kz.kimep.ticketing.identity.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService tickets;
    private final SalesService sales;
    private final CurrentUser current;

    @GetMapping("/tickets/{code}")
    @PreAuthorize("isAuthenticated()")
    public BookingDtos.TicketView ticket(@PathVariable String code) {
        return tickets.get(code, current.require().getId());
    }

    @GetMapping("/organizer/events/{eventId}/sales")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public BookingDtos.SalesView sales(@PathVariable Long eventId) {
        return sales.summary(eventId, current.require().getId());
    }
}
