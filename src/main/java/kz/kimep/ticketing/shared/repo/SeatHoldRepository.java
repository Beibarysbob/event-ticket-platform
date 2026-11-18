package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    java.util.List<SeatHold> findByStatusAndExpiresAtAfter(String status, java.time.Instant now);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select h from SeatHold h where h.holdReference = :ref")
    Optional<SeatHold> findLockedByReference(@org.springframework.data.repository.query.Param("ref") String ref);
    Optional<SeatHold> findByHoldReference(String ref);
}
