package kz.kimep.ticketing.shared.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kz.kimep.ticketing.shared.domain.Booking;
import kz.kimep.ticketing.shared.domain.Event;
import kz.kimep.ticketing.shared.domain.EventSectionPrice;
import kz.kimep.ticketing.shared.domain.Ticket;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.domain.Venue;
import kz.kimep.ticketing.shared.repo.BookingRepository;
import kz.kimep.ticketing.shared.repo.EventRepository;
import kz.kimep.ticketing.shared.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.shared.repo.SeatRepository;
import kz.kimep.ticketing.shared.repo.SectionRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import kz.kimep.ticketing.shared.repo.UserAccountRepository;
import kz.kimep.ticketing.shared.repo.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class DemoSeeder implements ApplicationRunner {
    private final UserAccountRepository users;
    private final VenueRepository venues;
    private final SectionRepository sections;
    private final SeatRepository seats;
    private final EventRepository events;
    private final EventSectionPriceRepository prices;
    private final BookingRepository bookings;
    private final TicketRepository tickets;
    private final PasswordEncoder passwords;
    @Value("${DEMO_PASSWORD:}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoPassword.isBlank()) {
            return;
        }
        createUsers();
        if (events.count() == 0) {
            createEvents();
            createBookings();
        }
    }

    private void createUsers() {
        createUser("admin@demo.local", "ADMIN");
        createUser("organizer1@demo.local", "ORGANIZER");
        createUser("organizer2@demo.local", "ORGANIZER");
        for (int number = 1; number <= 5; number++) {
            createUser("customer" + number + "@demo.local", "CUSTOMER");
        }
    }

    private void createUser(String email, String role) {
        if (users.findByEmail(email).isPresent()) {
            return;
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(demoPassword));
        user.setDisplayName(email.substring(0, email.indexOf('@')));
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        users.save(user);
    }

    private void createEvents() {
        List<Venue> catalog = venues.findAll();
        UserAccount organizer = users.findByEmail("organizer1@demo.local").orElseThrow();
        for (int number = 0; number < 10; number++) {
            Venue venue = catalog.get(number % catalog.size());
            Event event = new Event();
            event.setTitle("Demo Concert " + (number + 1));
            event.setDescription("University event ticketing demonstration");
            event.setVenue(venue);
            event.setOrganizer(organizer);
            event.setCreatedAt(Instant.now());
            event.setStartsAt(Instant.now().plus(number + 7, ChronoUnit.DAYS));
            event.setSalesOpenAt(Instant.now().minus(1, ChronoUnit.DAYS));
            event.setSalesCloseAt(Instant.now().plus(number + 6, ChronoUnit.DAYS));
            event.setStatus(number == 8 ? "SOLD_OUT" : number == 9 ? "DRAFT" : "ON_SALE");
            events.save(event);
            priceSections(event);
        }
    }

    private void priceSections(Event event) {
        sections.findByVenueId(event.getVenue().getId()).forEach(section -> {
            EventSectionPrice price = new EventSectionPrice();
            price.setEvent(event);
            price.setSection(section);
            price.setPrice(new BigDecimal("2000.00"));
            price.setCurrency("KZT");
            prices.save(price);
        });
    }

    private void createBookings() {
        List<Event> catalog = events.findAll();
        List<UserAccount> customers = users.findAll().stream()
            .filter(user -> "CUSTOMER".equals(user.getRole())).toList();
        for (int number = 0; number < 15; number++) {
            Event event = catalog.get(number % 8);
            UserAccount customer = customers.get(number % customers.size());
            Booking booking = new Booking();
            booking.setBookingReference("SEED-" + number);
            booking.setEvent(event);
            booking.setUser(customer);
            booking.setStatus(number % 3 == 0 ? "PENDING" : "CONFIRMED");
            booking.setCreatedAt(Instant.now());
            booking.setSubtotal(new BigDecimal("2000.00"));
            booking.setFees(BigDecimal.ZERO);
            booking.setTotal(new BigDecimal("2000.00"));
            booking.setCurrency("KZT");
            bookings.save(booking);
            createTicket(event, booking, number);
        }
    }

    private void createTicket(Event event, Booking booking, int number) {
        var section = sections.findByVenueId(event.getVenue().getId()).get(0);
        var seat = seats.findBySectionId(section.getId(), org.springframework.data.domain.PageRequest.of(0, 100))
            .getContent().get(number);
        Ticket ticket = new Ticket();
        ticket.setTicketCode("SEED-TICKET-" + number);
        ticket.setBooking(booking);
        ticket.setEvent(event);
        ticket.setSeat(seat);
        ticket.setStatus("CONFIRMED".equals(booking.getStatus()) ? "ISSUED" : "RESERVED");
        ticket.setPricePaid(new BigDecimal("2000.00"));
        tickets.save(ticket);
    }
}
