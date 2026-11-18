package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findTop20ByPublishedAtIsNullOrderByCreatedAtAsc();
}
