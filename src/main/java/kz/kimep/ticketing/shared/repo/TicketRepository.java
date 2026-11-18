package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @org.springframework.data.jpa.repository.Query("select t.seat.id from Ticket t where t.event.id = :eventId")
    List<Long> findSoldSeatIds(@org.springframework.data.repository.query.Param("eventId") Long eventId);
    boolean existsByEventIdAndSeatId(Long eventId, Long seatId);
    long countByEventIdAndStatus(Long eventId, String status);
    List<Ticket> findByBookingId(Long bookingId);
    Optional<Ticket> findByTicketCode(String code);
}
