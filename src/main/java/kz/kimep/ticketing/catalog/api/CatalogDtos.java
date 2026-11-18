package kz.kimep.ticketing.catalog.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class CatalogDtos {
    private CatalogDtos() { }
    public record SectionInput(@NotBlank String name, @Min(1) @Max(20) int rows,
                               @Min(1) @Max(25) int seatsPerRow) { }
    public record VenueInput(@NotBlank String name, String addressLine,
                             @NotBlank String city, String timezone,
                             @NotEmpty List<@Valid SectionInput> sections) { }
    public record VenueUpdate(@NotBlank String name, @NotBlank String city,
                              String addressLine) { }
    public record VenueView(Long id, String name, String city, int sections,
                            int capacity) implements Serializable { }
    public record EventInput(@NotBlank String title, String description,
                             @NotNull Instant startsAt, @NotNull Instant salesOpenAt,
                             @NotNull Instant salesCloseAt, @NotNull Long venueId,
                             @NotEmpty Map<Long, @DecimalMin("0.01") BigDecimal> prices) { }
    public record EventUpdate(@NotBlank String title, String description,
                              @NotNull Instant startsAt) { }
    public record EventView(Long id, String title, String status, Instant startsAt,
                            String venueName, String city) implements Serializable { }
    public record EventDetails(EventView event, Map<Long, BigDecimal> prices) implements Serializable { }
    public record EventQuery(String city, String status, Instant after, Instant before) { }
    public record PageView<T>(List<T> items, int page, int size,
                              long totalElements) implements Serializable { }
    public record Availability(Long sectionId, long freeSeats) implements Serializable { }
    public record SeatView(Long id, String row, int number, String status) implements Serializable { }
}
