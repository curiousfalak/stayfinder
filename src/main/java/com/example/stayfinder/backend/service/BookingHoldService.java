package com.example.stayfinder.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BookingHoldService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long HOLD_DURATION_SECONDS = 30;

    // Create a hold key from propertyId + dates
    private String buildHoldKey(UUID propertyId,
                                LocalDate checkIn, LocalDate checkOut) {
        return "hold:" + propertyId + ":" + checkIn + ":" + checkOut;
    }

    // Place a hold — returns false if already held
    public boolean placeHold(UUID propertyId,
                             LocalDate checkIn, LocalDate checkOut, UUID guestId) {
        String key = buildHoldKey(propertyId, checkIn, checkOut);

        // SETNX — set only if key does not exist
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, guestId.toString(),
                        HOLD_DURATION_SECONDS, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    // Release a hold manually (after booking confirmed or cancelled)
    public void releaseHold(UUID propertyId,
                            LocalDate checkIn, LocalDate checkOut) {
        String key = buildHoldKey(propertyId, checkIn, checkOut);
        redisTemplate.delete(key);
    }

    // Check if dates are currently held
    public boolean isHeld(UUID propertyId,
                          LocalDate checkIn, LocalDate checkOut) {
        String key = buildHoldKey(propertyId, checkIn, checkOut);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Get remaining hold time in seconds
    public long getHoldTtl(UUID propertyId,
                           LocalDate checkIn, LocalDate checkOut) {
        String key = buildHoldKey(propertyId, checkIn, checkOut);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
}