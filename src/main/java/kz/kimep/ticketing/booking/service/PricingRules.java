package kz.kimep.ticketing.booking.service;

import java.math.BigDecimal;
import java.util.List;
import kz.kimep.ticketing.shared.error.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PricingRules {
    public BigDecimal subtotal(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "At least one ticket price is required");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value == null || value.signum() <= 0) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "Every price must be positive");
            }
            sum = sum.add(value);
        }
        return sum;
    }
}
