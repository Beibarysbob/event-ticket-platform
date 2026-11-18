package kz.kimep.ticketing.shared.repo;

import kz.kimep.ticketing.shared.domain.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {
    boolean existsByMessageId(String messageId);
}
