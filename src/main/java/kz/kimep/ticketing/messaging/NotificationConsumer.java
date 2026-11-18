package kz.kimep.ticketing.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import kz.kimep.ticketing.shared.domain.ProcessedMessage;
import kz.kimep.ticketing.shared.domain.Ticket;
import kz.kimep.ticketing.shared.repo.ProcessedMessageRepository;
import kz.kimep.ticketing.shared.repo.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationConsumer.class);
    private final ObjectMapper mapper;
    private final ProcessedMessageRepository processed;
    private final TicketRepository tickets;

    @RabbitListener(queues = RabbitConfiguration.QUEUE)
    @Transactional
    public void receive(String payload) throws Exception {
        JsonNode message = mapper.readTree(payload);
        String id = message.path("messageId").asText();
        if (processed.existsByMessageId(id)) {
            return;
        }
        long bookingId = message.path("bookingId").asLong();
        String type = message.path("type").asText();
        if ("booking.confirmed".equals(type)) {
            issueTickets(bookingId);
        }
        ProcessedMessage receipt = new ProcessedMessage();
        receipt.setMessageId(id);
        receipt.setConsumedAt(Instant.now());
        processed.saveAndFlush(receipt);
        LOG.info("Asynchronous ticket event handled: {}", type);
    }

    private void issueTickets(long bookingId) {
        for (Ticket ticket : tickets.findByBookingId(bookingId)) {
            if ("RESERVED".equals(ticket.getStatus())) {
                ticket.setStatus("ISSUED");
                ticket.setIssuedAt(Instant.now());
            }
        }
    }
}
