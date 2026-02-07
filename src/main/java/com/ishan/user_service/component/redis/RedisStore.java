package com.ishan.user_service.component.redis;

import com.ishan.user_service.component.rateLimit.ImportJobCostTier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Low-level Redis access helper.
 * PURPOSE:
 * - Centralizes all direct Redis operations
 * - Hides StringRedisTemplate usage from business logic
 * - Makes Redis interactions explicit and readable
 * DESIGN LEARNING:
 * - Business logic should NOT know how Redis works
 * - It should only express intent (set key, check key, delete key)
 */
@Component
public class RedisStore {

    private final StringRedisTemplate redisTemplate;

    public RedisStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store a key with a value and TTL.
     *
     * Used for:
     * - job leases
     * - cooldowns
     *
     * TTL ensures Redis auto-cleans stale state.
     * TTL = Time To Live -> “How long something should exist before it disappears automatically.”
     * Example : TTL = 30 seconds, After 30 seconds → Redis deletes it by itself
     */
    public void setWithTtl(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Check whether a key exists.
     *
     * Used for:
     * - checking cooldown
     * - checking active job presence
     */
    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Delete a key explicitly.
     *
     * Used when:
     * - job finishes successfully
     * - cleanup is required before TTL expiry
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Counts active jobs for a given user and tier.
     *
     * PURPOSE:
     * - Used to enforce concurrency limits
     * - Count is derived from live job keys (presence = truth)
     *
     * DESIGN LEARNING:
     * - We do not store counters
     * - Redis keys + TTL make the system self-healing
     */
    public long countRunningJobs(String userId, ImportJobCostTier tier){
        String pattern = "job:"+userId+":"+tier.name()+":*";
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? 0 : keys.size();
    }

    public Long getTtlSeconds(String cooldownKey) {
        Long expire = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        if(expire == null || expire < 0){
            return 0L;
        }
        return expire;
    }

    /**
     * Reads the VALUE stored against a Redis key.
     *
     * PURPOSE:
     * - Used to retrieve value associated with a key
     *   (example: which tier triggered a cooldown)
     *
     * WHY THIS EXISTS:
     * - Redis keys define the scope (e.g. global cooldown)
     * - Redis values explain the reason/state (e.g. MEDIUM, LARGE)
     *
     * DESIGN LEARNING:
     * - Keys answer "what rule applies?"
     * - Values answer "why does it exist?"
     * - TTL answers "for how long?"
     */
    public String getValue(String key){
        return redisTemplate.opsForValue().get(key);
    }
}
