package com.ishan.user_service.component.rateLimit;

import com.ishan.user_service.component.redis.RedisStore;
import com.ishan.user_service.customExceptions.CooldownActiveException;
import com.ishan.user_service.customExceptions.TooManyRequestsException;
import com.ishan.user_service.utility.redis.RedisKeysGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Core decision engine for import job rate limiting.
 * Purpose:
 * - Decides if a user is allowed to start a new import job.
 * - Enforces concurrency limits per cost tier.
 * - Enforces cooldown after XL jobs.
 * Why Needed:
 * - Protects DB + CPU from overload.
 * - Ensures multi-tenant fairness.
 * Design:
 * - In-memory state (for now)
 * - Thread-safe storage using ConcurrentHashMap
 * Future:
 * - Can be replaced with Redis / DB backed state for distributed systems.
 */
@Service
public class ImportUsersRateLimitGuardService {

    private final Logger log = LoggerFactory.getLogger(ImportUsersRateLimitGuardService.class);

    //private final Map<String, UserRateState> userStateMap = new ConcurrentHashMap<>();

    private final RedisStore redisStore;

    public ImportUsersRateLimitGuardService(RedisStore redisStore) {
        this.redisStore = redisStore;
    }


    /**
     * MAIN ENTRY — decides if a job is allowed.
     *
     * Order matters:
     * Check GLOBAL cooldown (XL protection)
     * Check tier concurrency
     */
    public void checkIfAllowed(String userId, long count){

        checkCooldown(userId);
        ImportJobCostTier tier = ImportJobCostTier.fromCount(count);

        /*//COMMENTED: cooldown state moved from JVM memory to Redis
        UserRateState state = getOrCreateState(userId);
        LocalDateTime allowedAt = state.getNextImportAllowedAt();
        if(allowedAt != null && LocalDateTime.now().isBefore(allowedAt)){
            throw new TooManyRequestsException(
                    "You must wait until " + allowedAt + " before starting another import."
            );
        }*/
        //AtomicInteger running = state.getRunningJobs().computeIfAbsent(tier, t -> new AtomicInteger(0)); COMMENTED BECAUSE LOGIC SHIFTED FROM IM_MEMORY TO REDIS

        // Tier concurrency check
        long running = redisStore.countRunningJobs(userId, tier);
        if(running >= tier.getMaxConcurrentJobs()){
            String cooldownKey = RedisKeysGenerator.cooldownKey(userId);
            // COOLDOWN TTL Duration.ofSeconds (Rate-limiting purpose)
            // This TTL represents how long the USER must wait
            // after hitting a concurrency limit.
            // Meaning:
            // - System is overloaded for this user
            // - Block ALL job types during this window
            // - Duration depends on the tier that caused the overload
            // This TTL is about SYSTEM RECOVERY, not job execution.
            redisStore.setWithTtl(cooldownKey, tier.name(), Duration.ofSeconds(tier.getCooldownSeconds()));
            throw new TooManyRequestsException(
                    tier.name() + " concurrency limit reached. Max allowed = "
                            + tier.getMaxConcurrentJobs()
            );
        }
    }



    /**
     * MUST be called once job is ACCEPTED.
     * Uses atomic increment → thread safe.
     */
    public void markJobStarted(String userId, String jobId, ImportJobCostTier tier){

        String jobKey = RedisKeysGenerator.jobKey(userId,tier,jobId);
        // lease for the job; pick a safe max duration (e.g., 30 minutes for now)
        // JOB LEASE TTL (Safety purpose)
        // This TTL represents the maximum expected lifetime of a job.
        // Meaning:
        // - Job is considered running while this key exists
        // - If the app crashes or never calls markJobFinished(),
        //   Redis will auto-cleanup this job key
        // This TTL is about FAILURE SAFETY, not rate limiting.
        redisStore.setWithTtl(jobKey, tier.name(), Duration.ofMinutes(30));
        long running = redisStore.countRunningJobs(userId, tier);
        log.info("[RATE LIMIT GUARD]|User={} Tier={} Running={}",
                userId,
                tier,
                running);
    }



    /**
     * MUST be called in ASYNC finally block.
     *
     * Handles:
     * decrement
     * XL cooldown
     */
    public void markJobFinished(String userId, String jobId, ImportJobCostTier tier){

        String jobKey = RedisKeysGenerator.jobKey(userId, tier, jobId);
        redisStore.delete(jobKey);
    }

    /*private UserRateState getOrCreateState(String userId){
        return userStateMap.computeIfAbsent(userId, id -> new UserRateState());
    }*/

    /**
     * Checks whether the user is currently in cooldown.
     *
     * PURPOSE:
     * - Enforces a global cooldown triggered when any tier hits its concurrency limit
     * - Uses Redis TTL to auto-expire cooldown
     *
     * DESIGN LEARNING:
     * - Cooldown is shared, time-based state
     * - Redis is the source of truth, not memory
     */
    private void checkCooldown(String userId) {
        String cooldownKey = RedisKeysGenerator.cooldownKey(userId);

        if(!redisStore.exists(cooldownKey)){
            return;
        }
        Long ttlSeconds = redisStore.getTtlSeconds(cooldownKey);
        String value = redisStore.getValue(cooldownKey);
        ImportJobCostTier tier = ImportJobCostTier.valueOf(value);
        int totalTime =  tier.getCooldownSeconds();
        if (redisStore.exists(cooldownKey)) {
            throw new CooldownActiveException("User is in cooldown period", totalTime, ttlSeconds);
        }
    }

}

