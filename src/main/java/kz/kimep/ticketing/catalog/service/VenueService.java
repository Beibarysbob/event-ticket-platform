package kz.kimep.ticketing.catalog.service;

import java.util.ArrayList;
import java.util.List;
import kz.kimep.ticketing.catalog.api.CatalogDtos;
import kz.kimep.ticketing.shared.domain.Seat;
import kz.kimep.ticketing.shared.domain.Section;
import kz.kimep.ticketing.shared.domain.Venue;
import kz.kimep.ticketing.shared.error.DomainException;
import kz.kimep.ticketing.shared.repo.EventRepository;
import kz.kimep.ticketing.shared.repo.SeatRepository;
import kz.kimep.ticketing.shared.repo.SectionRepository;
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
public class VenueService {
    private final VenueRepository venues;
    private final SectionRepository sections;
    private final SeatRepository seats;
    private final EventRepository events;

    @Transactional(readOnly = true)
    @Cacheable(value = "venues", key = "#id")
    public CatalogDtos.VenueView get(Long id) {
        Venue venue = require(id);
        return view(venue);
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageView<CatalogDtos.VenueView> list(int page, int size) {
        var result = venues.findAll(PageRequest.of(page, Math.min(size, 50)));
        return new CatalogDtos.PageView<>(result.map(this::view).getContent(),
            page, Math.min(size, 50), result.getTotalElements());
    }

    @Transactional
    public CatalogDtos.VenueView create(CatalogDtos.VenueInput request) {
        Venue venue = new Venue();
        venue.setName(request.name());
        venue.setCity(request.city());
        venue.setTimezone(request.timezone());
        venue.setAddressLine(request.addressLine());
        int capacity = request.sections().stream().mapToInt(s -> s.rows() * s.seatsPerRow()).sum();
        venue.setTotalCapacity(capacity);
        venue = venues.save(venue);
        for (var specification : request.sections()) {
            createSection(venue, specification);
        }
        return view(venue);
    }

    @Transactional
    @CacheEvict(value = "venues", key = "#id")
    public CatalogDtos.VenueView update(Long id, CatalogDtos.VenueUpdate request) {
        Venue venue = require(id);
        venue.setName(request.name());
        venue.setCity(request.city());
        venue.setAddressLine(request.addressLine());
        return view(venue);
    }

    @Transactional
    @CacheEvict(value = "venues", key = "#id")
    public void delete(Long id) {
        Venue venue = require(id);
        if (events.existsByVenueId(id)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Venue has events; remove events first");
        }
        for (Section section : sections.findByVenueId(id)) {
            seats.deleteAll(seats.findAllBySectionId(section.getId()));
            sections.delete(section);
        }
        venues.delete(venue);
    }

    private Venue require(Long id) {
        return venues.findById(id).orElseThrow(() ->
            new DomainException(HttpStatus.NOT_FOUND, "Venue was not found"));
    }

    private CatalogDtos.VenueView view(Venue venue) {
        return new CatalogDtos.VenueView(venue.getId(), venue.getName(), venue.getCity(),
            sections.findByVenueId(venue.getId()).size(), venue.getTotalCapacity());
    }

    private void createSection(Venue venue, CatalogDtos.SectionInput input) {
        Section section = new Section();
        section.setVenue(venue);
        section.setName(input.name());
        section.setRowCount(input.rows());
        section.setSeatsPerRow(input.seatsPerRow());
        section.setPriceTier("STANDARD");
        section = sections.save(section);
        List<Seat> group = new ArrayList<>();
        for (int row = 1; row <= input.rows(); row++) {
            for (int number = 1; number <= input.seatsPerRow(); number++) {
                Seat seat = new Seat();
                seat.setSection(section);
                seat.setRowLabel(String.valueOf((char) ('A' + row - 1)));
                seat.setSeatNumber(number);
                group.add(seat);
            }
        }
        seats.saveAll(group);
    }
}
