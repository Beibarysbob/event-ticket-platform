package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.catalog.service.EventService;
import kz.kimep.ticketing.identity.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService service;
    private final CurrentUser current;

    @GetMapping
    public CatalogDtos.PageView<CatalogDtos.EventView> list(
            @org.springframework.web.bind.annotation.ModelAttribute CatalogDtos.EventQuery query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(query, page, size);
    }

    @GetMapping("/{id}")
    public CatalogDtos.EventDetails get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<CatalogDtos.EventDetails> create(@Valid @RequestBody CatalogDtos.EventInput request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, current.require()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public CatalogDtos.EventDetails update(@PathVariable Long id, @Valid @RequestBody CatalogDtos.EventUpdate request) {
        return service.update(id, request, current.require().getId());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public CatalogDtos.EventDetails publish(@PathVariable Long id) {
        return service.publish(id, current.require().getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, current.require().getId());
        return ResponseEntity.noContent().build();
    }
}
