package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.time.Instant;

public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByVenueId(Long venueId);
    @EntityGraph(attributePaths = {"venue", "organizer"})
    @Query("select e from Event e where (:city is null or lower(e.venue.city) like lower(concat('%', :city, '%'))) and (:status is null or e.status = :status) and (:after is null or e.startsAt >= :after) and (:before is null or e.startsAt <= :before)")
    Page<Event> search(@Param("city") String city, @Param("status") String status, @Param("after") Instant after, @Param("before") Instant before, Pageable pageable);
    @EntityGraph(attributePaths = {"venue", "organizer"})
    Optional<Event> findWithVenueById(Long id);
}
