package kz.kimep.ticketing.booking.service;

import java.util.ArrayList;
import java.util.List;
import kz.kimep.ticketing.catalog.api.CatalogDtos;
import kz.kimep.ticketing.catalog.service.EventService;
import kz.kimep.ticketing.shared.domain.Seat;
import kz.kimep.ticketing.shared.domain.Section;
import kz.kimep.ticketing.shared.repo.SeatRepository;
import kz.kimep.ticketing.shared.repo.SectionRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final EventService events;
    private final SectionRepository sections;
    private final SeatRepository seats;
    private final TicketRepository tickets;
    private final SeatLockService locks;

    @Transactional(readOnly = true)
    public List<CatalogDtos.Availability> summary(Long eventId) {
        var event = events.require(eventId);
        List<Seat> all = seats.findBySectionVenueId(event.getVenue().getId());
        java.util.Set<Long> sold = new java.util.HashSet<>(tickets.findSoldSeatIds(eventId));
        java.util.Set<Long> held = locks.heldSeatIds(eventId, all.stream().map(Seat::getId).toList());
        List<CatalogDtos.Availability> counts = new ArrayList<>();
        for (Section section : sections.findByVenueId(event.getVenue().getId())) {
            long available = all.stream().filter(seat -> seat.getSection().getId().equals(section.getId()))
                .filter(seat -> !sold.contains(seat.getId()) && !held.contains(seat.getId())).count();
            counts.add(new CatalogDtos.Availability(section.getId(), available));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public CatalogDtos.PageView<CatalogDtos.SeatView> seats(Long eventId, Long sectionId, int page, int size) {
        var event = events.require(eventId);
        var section = sections.findById(sectionId).orElseThrow(() ->
            new kz.kimep.ticketing.shared.error.DomainException(org.springframework.http.HttpStatus.NOT_FOUND, "Section was not found"));
        if (!section.getVenue().getId().equals(event.getVenue().getId())) {
            throw new kz.kimep.ticketing.shared.error.DomainException(org.springframework.http.HttpStatus.BAD_REQUEST, "Section is not in venue");
        }
        int limited = Math.min(50, size);
        var result = seats.findBySectionId(sectionId, PageRequest.of(page, limited));
        java.util.Set<Long> sold = new java.util.HashSet<>(tickets.findSoldSeatIds(eventId));
        java.util.Set<Long> held = locks.heldSeatIds(eventId,
            result.getContent().stream().map(Seat::getId).toList());
        return new CatalogDtos.PageView<>(result.map(seat ->
            new CatalogDtos.SeatView(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(),
                sold.contains(seat.getId()) ? "SOLD" : held.contains(seat.getId()) ? "HELD" : "FREE"))
            .getContent(), page, limited, result.getTotalElements());
    }

}
