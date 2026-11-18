package kz.kimep.ticketing.catalog.api;

import java.util.List;
import kz.kimep.ticketing.booking.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService availability;

    @GetMapping("/availability")
    public List<CatalogDtos.Availability> summary(@PathVariable Long eventId) {
        return availability.summary(eventId);
    }

    @GetMapping("/seats")
    public CatalogDtos.PageView<CatalogDtos.SeatView> seats(@PathVariable Long eventId,
            @RequestParam Long sectionId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return availability.seats(eventId, sectionId, page, size);
    }
}
