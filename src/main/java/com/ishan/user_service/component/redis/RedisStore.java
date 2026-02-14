package com.ishan.user_service.component.redis;

import com.ishan.user_service.component.rateLimit.ImportJobCostTier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
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
    private final DefaultRedisScript<Long> checkAndAddRunningJobScript;

    public RedisStore(StringRedisTemplate redisTemplate, DefaultRedisScript<Long> checkAndAddRunningJobScript) {
        this.redisTemplate = redisTemplate;
        this.checkAndAddRunningJobScript = checkAndAddRunningJobScript;
    }

    /**
     * Stores a key in Redis with a value(Tier Name) and an automatic expiry time (TTL).
     *
     * SIMPLE MEANING:
     * - We save some information in Redis
     * - Redis will automatically delete it after the given time
     *
     * WHY WE USE THIS:
     * - Prevents stale data if the application crashes
     * - Avoids manual cleanup jobs
     * - Keeps Redis memory clean automatically
     *
     * WHERE IT IS USED:
     * 1) Job safety key
     *    → If a job starts but never finishes (app crash),
     *      Redis will auto-remove it after TTL.
     *
     * 2) User cooldown
     *    → Temporarily blocks a user for a fixed time window.
     *
     * TTL (Time To Live):
     * - Defines how long the key should exist.
     * - Example: TTL = 30 seconds
     *   → After 30 seconds Redis deletes the key automatically.
     *
     * IMPORTANT LEARNING:
     * TTL is our safety net against stuck or orphaned state.
     */
    public void setWithTtl(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Check whether a key exists.
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
        // Idempotent Redis operation — safely deletes the key; repeated calls have no side effects if the key is already absent
        redisTemplate.delete(key);
    }

    /**
     * Counts active jobs for a given user and tier.
     *
     * PURPOSE:
     * - Used to enforce concurrency limits
     * - Count is derived from live job keys (presence = truth)
     *
     *  DEPRECATED:
     * Because used KEYS() pattern scan which is O(N) and blocks Redis.
     * Replaced with Redis SET based counting for production safety.
     */
   /* public long countRunningJobs(String userId, ImportJobCostTier tier){
        String pattern = "job:"+userId+":"+tier.name()+":*";
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? 0 : keys.size();
    }*/

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

    /**
     * PURPOSE:
     * Adds a jobId into the Redis SET that tracks running jobs
     * for a specific user and tier.
     *
     * CURRENT STATUS:
     * - Not used in the main rate limiter flow.
     * - Slot reservation is now handled atomically via Lua.
     *
     * WHY KEPT:
     * - Useful for testing, recovery, or manual admin operations.
     * - Acts as a low-level helper if Lua path is ever bypassed.
     *
     * IMPORTANT:
     * - Do NOT use this in the normal admission path,
     *   otherwise it will break atomic guarantees.
     */
    public void insertUserRunningJobsInsideSet(String key, String jobId){
        redisTemplate.opsForSet().add(key, jobId);
    }

    /**
     * PURPOSE:
     * Removes a completed jobId from the Redis SET of running jobs.
     *
     * WHY THIS EXISTS:
     * - Keeps concurrency count accurate after job completion.
     * - Prevents stale entries that could falsely block users.
     *
     * DESIGN LEARNING:
     * - We remove only the specific jobId (NOT the whole key).
     * - Works together with TTL safety on the job key.
     * - Required for correctness in distributed environments.
     *
     */

    public void deleteUserRunningJobsFromSet(String key, String jobId){
        // Idempotent Redis operation — safely removes jobId from SET; repeated calls have no side effects and keep state consistent
        redisTemplate.opsForSet().remove(key, jobId);

    }
    /**
     * PURPOSE:
     * Returns the current number of running jobs for a given user+tier
     * by reading the Redis SET size.
     *
     * WHY THIS EXISTS:
     * - Replaces the old KEYS() pattern scan (which is O(N) and blocks Redis).
     * - Uses Redis SCARD via SET size for fast and production-safe counting.
     *
     * DESIGN LEARNING:
     * - Redis SET is our source of truth for concurrency tracking.
     * - SCARD (size) is an O(1) operation — safe under high load.
     * - RedisTemplate.size() may return null if key does not exist,
     *   so we defensively return 0.
     *
     * RESULT:
     * - Accurate per-user per-tier concurrency count
     * - Non-blocking Redis usage
     * - Production-grade scalability
     */
    public long countPerUserRunningJobsFromSet(String key){
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    /**
     * Runs a Lua script in Redis to atomically:
     * 1) check current running job count
     * 2) compare it with the allowed limit
     * 3) if allowed → add this jobId into the Redis SET
     *
     * WHY THIS METHOD EXISTS:
     * - Prevents race conditions when multiple requests hit at the same time
     * - Ensures check + add happens as ONE single operation inside Redis
     * - Avoids over-booking of job slots
     *
     * PARAMETERS:
     * runningJobsKey → Redis SET key holding running jobIds for user+tier
     * limit          → maximum allowed concurrent jobs for this tier
     * jobId          → unique job identifier to reserve slot
     *
     * RETURN:
     * true  → slot reserved successfully (job can start)
     * false → limit already reached (job must be rejected)
     *
     * IMPORTANT LEARNING:
     * - We do NOT trust JVM memory for concurrency in distributed systems
     * - Redis Lua gives us strong atomic behavior
     */
    public boolean executeLuaScriptToCheckIfAllowed(String runningJobsKey, int limit, String jobId){
        Long result = redisTemplate.execute(
                checkAndAddRunningJobScript,
                Collections.singletonList(runningJobsKey),
                String.valueOf(limit),
                jobId
        );
        // Lua returns 1 = allowed, 0 = rejected
        return result != null && result == 1L;
    }
}
