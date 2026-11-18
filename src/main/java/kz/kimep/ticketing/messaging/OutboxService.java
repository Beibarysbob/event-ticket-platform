package kz.kimep.ticketing.messaging;

import java.time.Instant;
import kz.kimep.ticketing.shared.domain.Booking;
import kz.kimep.ticketing.shared.domain.OutboxMessage;
import kz.kimep.ticketing.shared.repo.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxMessageRepository outbox;

    public void record(Booking booking, String type) {
        OutboxMessage message = new OutboxMessage();
        message.setAggregateType("Booking");
        message.setAggregateId(booking.getId());
        message.setEventType(type);
        message.setPayload("{\"bookingReference\":\"" + booking.getBookingReference() + "\"}");
        message.setCreatedAt(Instant.now());
        outbox.save(message);
    }
}
