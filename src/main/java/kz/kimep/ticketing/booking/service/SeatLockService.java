package kz.kimep.ticketing.booking.service;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class SeatLockService {
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class);
    private final StringRedisTemplate redis;

    public SeatLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean claim(Long eventId, Long seatId, String reference, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(eventId, seatId), reference, ttl));
    }

    public java.util.Set<Long> heldSeatIds(Long eventId, List<Long> seatIds) {
        List<String> keys = seatIds.stream().map(id -> key(eventId, id)).toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        java.util.Set<Long> held = new java.util.HashSet<>();
        if (values != null) {
            for (int index = 0; index < values.size(); index++) {
                if (values.get(index) != null) {
                    held.add(seatIds.get(index));
                }
            }
        }
        return held;
    }

    public boolean isHeld(Long eventId, Long seatId) {
        return Boolean.TRUE.equals(redis.hasKey(key(eventId, seatId)));
    }

    public void release(Long eventId, List<Long> seatIds, String reference) {
        for (Long seatId : seatIds) {
            redis.execute(RELEASE, List.of(key(eventId, seatId)), reference);
        }
    }

    public static String key(Long eventId, Long seatId) {
        return "hold:" + eventId + ":" + seatId;
    }
}
