package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Page<Venue> findByCityContainingIgnoreCase(String city, Pageable pageable);
}
