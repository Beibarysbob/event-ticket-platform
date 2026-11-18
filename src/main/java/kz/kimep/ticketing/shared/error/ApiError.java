package kz.kimep.ticketing.shared.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant time, int status, String error, String message, Map<String, String> fields) { }
