package kz.kimep.ticketing.booking.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.booking.service.BookingService;
import kz.kimep.ticketing.catalog.api.CatalogDtos;
import kz.kimep.ticketing.identity.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {
    private final BookingService service;
    private final CurrentUser current;

    @PostMapping
    public ResponseEntity<BookingDtos.BookingView> create(@Valid @RequestBody BookingDtos.BookingInput request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(request.holdReference(), current.require()));
    }

    @GetMapping
    public CatalogDtos.PageView<BookingDtos.BookingView> list(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return service.list(current.require().getId(), page, size);
    }

    @GetMapping("/{reference}")
    public BookingDtos.BookingView get(@PathVariable String reference) {
        return service.get(reference, current.require().getId());
    }

    @PostMapping("/{reference}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String reference) {
        service.cancel(reference, current.require().getId());
        return ResponseEntity.noContent().build();
    }
}
