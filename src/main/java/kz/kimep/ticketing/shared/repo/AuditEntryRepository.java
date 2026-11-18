package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
    List<AuditEntry> findTop20ByActorIdOrderByCreatedAtDesc(Long actorId);
}
