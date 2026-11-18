package kz.kimep.ticketing;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import kz.kimep.ticketing.catalog.api.CatalogDtos;
import kz.kimep.ticketing.catalog.service.EventService;
import kz.kimep.ticketing.shared.domain.Event;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.repo.EventRepository;
import kz.kimep.ticketing.shared.repo.UserAccountRepository;
import kz.kimep.ticketing.shared.repo.VenueRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventQueryCountIT extends IntegrationContainers {
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private EventRepository events;
    @Autowired private EventService service;
    @Autowired private UserAccountRepository users;
    @Autowired private VenueRepository venues;

    @Test
    void eventQueryCountDoesNotGrowWithPageSize() {
        String titlePrefix = "PERF-" + UUID.randomUUID();
        createEvents(titlePrefix);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        service.search(new CatalogDtos.EventQuery(null, "PERF", null, null), 0, 10);
        long forTen = statistics.getPrepareStatementCount();
        statistics.clear();
        service.search(new CatalogDtos.EventQuery(null, "PERF", null, null), 0, 50);
        long forFifty = statistics.getPrepareStatementCount();
        assertTrue(forTen <= 3, "N+1 detected: " + forTen);
        assertEquals(forTen, forFifty, "Query count must not depend on result size");
    }

    private void createEvents(String prefix) {
        UserAccount user = new UserAccount();
        user.setEmail(UUID.randomUUID() + "@example.org");
        user.setDisplayName("Performance");
        user.setPasswordHash("test-only");
        user.setRole("ORGANIZER");
        user.setEnabled(true);
        user = users.save(user);
        var venue = venues.findById(1L).orElseThrow();
        for (int number = 0; number < 60; number++) {
            Event event = new Event();
            event.setTitle(prefix + number);
            event.setVenue(venue);
            event.setOrganizer(user);
            event.setStartsAt(Instant.now().plusSeconds(86400));
            event.setStatus("PERF");
            events.save(event);
        }
    }
}
