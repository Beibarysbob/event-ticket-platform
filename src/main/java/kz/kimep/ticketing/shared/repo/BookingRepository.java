package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(b.total), 0) from Booking b where b.event.id = :eventId and b.status = 'CONFIRMED'")
    java.math.BigDecimal totalSales(@org.springframework.data.repository.query.Param("eventId") Long eventId);
    Optional<Booking> findByBookingReference(String ref);
    Page<Booking> findByUserId(Long id, Pageable pageable);
    long countByEventIdAndStatus(Long eventId, String status);
    boolean existsByEventId(Long eventId);
}
