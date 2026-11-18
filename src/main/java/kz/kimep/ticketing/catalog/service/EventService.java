package kz.kimep.ticketing.catalog.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import kz.kimep.ticketing.catalog.api.CatalogDtos;
import kz.kimep.ticketing.shared.domain.Event;
import kz.kimep.ticketing.shared.domain.EventSectionPrice;
import kz.kimep.ticketing.shared.domain.UserAccount;
import kz.kimep.ticketing.shared.domain.Venue;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.EventRepository;
import kz.kimep.ticketing.shared.repo.BookingRepository;
import kz.kimep.ticketing.shared.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.shared.repo.SectionRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import kz.kimep.ticketing.shared.repo.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository events;
    private final VenueRepository venues;
    private final SectionRepository sections;
    private final EventSectionPriceRepository prices;
    private final TicketRepository tickets;
    private final BookingRepository bookings;

    @Transactional(readOnly = true)
    @Cacheable(value = "eventLists", key = "#query.toString() + ':' + #page + ':' + #size")
    public CatalogDtos.PageView<CatalogDtos.EventView> search(CatalogDtos.EventQuery query, int page, int size) {
        int limited = Math.min(size, 50);
        var result = events.search(query.city(), query.status(), query.after(),
            query.before(), PageRequest.of(page, limited));
        return new CatalogDtos.PageView<>(result.map(this::summary).getContent(),
            page, limited, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "events", key = "#id")
    public CatalogDtos.EventDetails get(Long id) {
        Event event = require(id);
        Map<Long, BigDecimal> values = new HashMap<>();
        prices.findByEventId(id).forEach(p -> values.put(p.getSection().getId(), p.getPrice()));
        return new CatalogDtos.EventDetails(summary(event), values);
    }

    @Transactional
    @CacheEvict(value = {"events", "eventLists"}, allEntries = true)
    public CatalogDtos.EventDetails create(CatalogDtos.EventInput input, UserAccount organizer) {
        requireDates(input.salesOpenAt(), input.salesCloseAt(), input.startsAt());
        Venue venue = venues.findById(input.venueId()).orElseThrow(() ->
            new DomainException(HttpStatus.BAD_REQUEST, "venueId does not exist"));
        Event event = new Event();
        event.setTitle(input.title());
        event.setDescription(input.description());
        event.setStartsAt(input.startsAt());
        event.setSalesOpenAt(input.salesOpenAt());
        event.setSalesCloseAt(input.salesCloseAt());
        event.setVenue(venue);
        event.setOrganizer(organizer);
        event.setStatus("DRAFT");
        event.setCreatedAt(Instant.now());
        event = events.save(event);
        savePrices(event, input.prices());
        return getUncached(event);
    }

    @Transactional
    @CacheEvict(value = {"events", "eventLists"}, allEntries = true)
    public CatalogDtos.EventDetails update(Long id, CatalogDtos.EventUpdate input, Long userId) {
        Event event = requireOwned(id, userId);
        if (!"DRAFT".equals(event.getStatus())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Only draft events can be edited");
        }
        event.setTitle(input.title());
        event.setDescription(input.description());
        event.setStartsAt(input.startsAt());
        return getUncached(event);
    }

    @Transactional
    @CacheEvict(value = {"events", "eventLists"}, allEntries = true)
    public CatalogDtos.EventDetails publish(Long id, Long userId) {
        Event event = requireOwned(id, userId);
        if (!"DRAFT".equals(event.getStatus())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Event is already published");
        }
        event.setStatus("ON_SALE");
        return getUncached(event);
    }

    @Transactional
    @CacheEvict(value = {"events", "eventLists"}, allEntries = true)
    public void delete(Long id, Long userId) {
        Event event = requireOwned(id, userId);
        if (bookings.existsByEventId(id)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Cannot delete an event with bookings");
        }
        prices.deleteAll(prices.findByEventId(id));
        events.delete(event);
    }

    @Transactional(readOnly = true)
    public Event require(Long id) {
        return events.findWithVenueById(id).orElseThrow(() ->
            new DomainException(HttpStatus.NOT_FOUND, "Event was not found"));
    }

    @Transactional(readOnly = true)
    public Event requireOwned(Long id, Long userId) {
        Event event = require(id);
        if (!event.getOrganizer().getId().equals(userId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Event was not found");
        }
        return event;
    }

    public void requireDates(Instant open, Instant close, Instant starts) {
        if (!open.isBefore(close) || !close.isBefore(starts)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "salesOpenAt must precede salesCloseAt and startsAt");
        }
    }

    private CatalogDtos.EventView summary(Event event) {
        return new CatalogDtos.EventView(event.getId(), event.getTitle(), event.getStatus(),
            event.getStartsAt(), event.getVenue().getName(), event.getVenue().getCity());
    }

    private CatalogDtos.EventDetails getUncached(Event event) {
        Map<Long, BigDecimal> values = new HashMap<>();
        prices.findByEventId(event.getId()).forEach(p -> values.put(p.getSection().getId(), p.getPrice()));
        return new CatalogDtos.EventDetails(summary(event), values);
    }

    private void savePrices(Event event, Map<Long, BigDecimal> requested) {
        var available = sections.findByVenueId(event.getVenue().getId());
        if (!requested.keySet().equals(available.stream().map(s -> s.getId()).collect(java.util.stream.Collectors.toSet()))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Provide prices for exactly every venue section");
        }
        available.forEach(section -> {
            EventSectionPrice price = new EventSectionPrice();
            price.setEvent(event);
            price.setSection(section);
            price.setPrice(requested.get(section.getId()));
            price.setCurrency("KZT");
            prices.save(price);
        });
    }
}
