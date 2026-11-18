package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"section"})
    List<Seat> findBySectionVenueId(Long venueId);
    Page<Seat> findBySectionId(Long sectionId, Pageable pageable);
    List<Seat> findAllBySectionId(Long sectionId);
    List<Seat> findByIdIn(List<Long> ids);
}
