package kz.kimep.ticketing.payment;

import jakarta.validation.Valid;
import kz.kimep.ticketing.booking.api.BookingDtos;
import kz.kimep.ticketing.identity.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;
    private final CurrentUser current;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDtos.BookingView> pay(@Valid @RequestBody BookingDtos.PaymentInput request) {
        BookingDtos.BookingView result = payments.pay(request, current.require().getId());
        return ResponseEntity.status("CANCELLED".equals(result.status())
            ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK).body(result);
    }
}
