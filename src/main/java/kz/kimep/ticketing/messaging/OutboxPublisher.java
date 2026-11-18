package kz.kimep.ticketing.messaging;

import java.time.Instant;
import kz.kimep.ticketing.shared.domain.OutboxMessage;
import kz.kimep.ticketing.shared.repo.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxMessageRepository outbox;
    private final RabbitTemplate rabbit;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishPending() {
        for (OutboxMessage message : outbox.findTop20ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                String payload = "{\"messageId\":\"" + message.getId()
                    + "\",\"type\":\"" + message.getEventType()
                    + "\",\"bookingId\":" + message.getAggregateId() + "}";
                rabbit.convertAndSend(RabbitConfiguration.EXCHANGE, message.getEventType(), payload);
                message.setPublishedAt(Instant.now());
            } catch (RuntimeException problem) {
                LOG.warn("Message broker unavailable; outbox row {} will retry", message.getId());
                return;
            }
        }
    }
}
