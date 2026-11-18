package kz.kimep.ticketing.shared.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalErrorHandler {
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> domain(DomainException error) {
        HttpStatus status = error.getStatus();
        return ResponseEntity.status(status).body(build(status, error.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(item ->
            fields.putIfAbsent(item.getField(), item.getDefaultMessage()));
        return ResponseEntity.badRequest().body(build(HttpStatus.BAD_REQUEST, "Correct the fields listed", fields));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> malformed(Exception error) {
        return ResponseEntity.badRequest().body(build(HttpStatus.BAD_REQUEST, "Invalid request body or parameter", Map.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(build(HttpStatus.FORBIDDEN, "You do not have permission", Map.of()));
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
        org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> conflict(Exception error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(build(HttpStatus.CONFLICT, "Seat or data changed; retry the request", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception error) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", Map.of()));
    }

    private ApiError build(HttpStatus status, String message, Map<String, String> fields) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fields);
    }
}
