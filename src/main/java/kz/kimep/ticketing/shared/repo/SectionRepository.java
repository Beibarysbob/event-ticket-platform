package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByVenueId(Long venueId);
}
