package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.EventSectionPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface EventSectionPriceRepository extends JpaRepository<EventSectionPrice, Long> {
    List<EventSectionPrice> findByEventId(Long eventId);
    Optional<EventSectionPrice> findByEventIdAndSectionId(Long eventId, Long sectionId);
}
