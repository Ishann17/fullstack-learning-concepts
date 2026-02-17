package com.ishan.user_service.component.redis;

import com.ishan.user_service.component.rateLimit.ImportJobCostTier;
import com.ishan.user_service.utility.redis.RedisKeysGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisKeyExpiryListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisKeyExpiryListener.class);
    private final RedisStore redisStore;

    public RedisKeyExpiryListener(RedisStore redisStore) {
        this.redisStore = redisStore;
    }


    /**
     * Handles Redis key expiration events and performs self-healing cleanup.
     *
     * PURPOSE:
     * - Listens for TTL expiry of job safety keys.
     * - When a job key expires (usually due to crash or failure),
     *   this method removes the stale jobId from the running jobs SET.
     * - Ensures concurrency slots are eventually freed.
     *
     * WHEN THIS TRIGGERS:
     * Redis key expires:
     *   job:{userId}:{tier}:{jobId}
     * → Redis publishes expiry event
     * → RedisMessageListenerContainer receives it
     * → This method is invoked
     *
     * HIGH-LEVEL FLOW:
     * 1. Extract expired key from Redis message.
     * 2. Filter only job-related keys (ignore others).
     * 3. Validate key structure defensively.
     * 4. Parse userId, tier, jobId.
     * 5. Reconstruct running jobs SET key.
     * 6. Remove stale jobId from the SET.
     *
     * WHY DEFENSIVE CHECKS EXIST:
     * - Redis publishes expiry for ALL keys, not just ours.
     * - Malformed keys must never break the listener.
     * - Listener must be fail-safe and idempotent.
     *
     * IMPORTANT DESIGN NOTE:
     * - Normal successful jobs call markJobFinished() and clean immediately.
     * - This listener is the FAILURE SAFETY NET (crash recovery path).
     * - Pub/Sub delivery is best-effort, so periodic sweeper may be added in future.
     *
     * RESULT:
     * - Prevents ghost jobs in Redis.
     * - Maintains accurate concurrency tracking.
     * - Makes rate limiter self-healing in distributed environments.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] body = message.getBody();
        String expiredKey = new String(body, StandardCharsets.UTF_8);

        if(!expiredKey.startsWith("job:")){
            log.info("[REDIS_KEY_EXPIRE_LISTENER] Ignoring malformed job key | key={}", expiredKey);
            return;
        }

        String[] parsedKey = expiredKey.split(":");

        if(parsedKey.length != 4){
            log.warn("[REDIS_KEY_EXPIRE_LISTENER] Ignoring malformed job key | key={}", expiredKey);
            return;
        }

        //parsing expired key to redis key

        String userId = parsedKey[1];
        String enumTier = parsedKey[2];
        String jobId = parsedKey[3];
        log.info("[REDIS_KEY_EXPIRE_LISTENER] Parsed expired job | userId={} tier={} jobId={}", userId, enumTier, jobId);

        //String to ENUM
        ImportJobCostTier tier = ImportJobCostTier.valueOf(enumTier);
        String userRunningJobsKey = RedisKeysGenerator.userRunningJobsKey(userId, tier);

        redisStore.deleteUserRunningJobsFromSet(userRunningJobsKey, jobId);
        log.info("[RATE_LIMIT][EXPIRE_LISTENER] Cleaned stale job | userId={} tier={} jobId={}", userId, tier, jobId);
    }
}
