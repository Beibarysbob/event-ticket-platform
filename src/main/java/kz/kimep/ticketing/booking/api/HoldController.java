package kz.kimep.ticketing.booking.api;

import jakarta.validation.Valid;
import java.net.URI;
import kz.kimep.ticketing.booking.service.HoldService;
import kz.kimep.ticketing.identity.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holds")
@RequiredArgsConstructor
public class HoldController {
    private final HoldService service;
    private final CurrentUser current;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ORGANIZER', 'ADMIN')")
    public ResponseEntity<BookingDtos.HoldView> create(@Valid @RequestBody BookingDtos.HoldInput request) {
        BookingDtos.HoldView result = service.hold(request, current.require());
        return ResponseEntity.created(URI.create("/api/v1/holds/" + result.reference())).body(result);
    }

    @GetMapping("/{reference}")
    @PreAuthorize("isAuthenticated()")
    public BookingDtos.HoldView get(@PathVariable String reference) {
        return service.get(reference, current.require().getId());
    }

    @DeleteMapping("/{reference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> release(@PathVariable String reference) {
        service.release(reference, current.require().getId());
        return ResponseEntity.noContent().build();
    }
}
