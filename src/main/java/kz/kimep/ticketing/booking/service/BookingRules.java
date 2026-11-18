package kz.kimep.ticketing.booking.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import kz.kimep.ticketing.shared.error.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class BookingRules {
    public void requireDistinctSeats(List<Long> ids, int max) {
        if (ids == null || ids.isEmpty() || ids.size() > max
                || ids.contains(null) || new HashSet<>(ids).size() != ids.size()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Choose distinct seats within the allowed limit");
        }
    }

    public void requireLive(String status, Instant expires, Instant now) {
        if (!"HELD".equals(status)) {
            throw new DomainException(HttpStatus.CONFLICT, "Hold was already used or released");
        }
        if (expires == null || !now.isBefore(expires)) {
            throw new DomainException(HttpStatus.GONE, "Hold has expired; choose seats again");
        }
    }

    public void requirePending(String status) {
        if (!"PENDING".equals(status)) {
            throw new DomainException(HttpStatus.CONFLICT, "Booking is not pending");
        }
    }

    public boolean isSaleOpen(String status, Instant open, Instant close, Instant now) {
        return "ON_SALE".equals(status) && open != null && close != null
            && !now.isBefore(open) && now.isBefore(close);
    }
}
