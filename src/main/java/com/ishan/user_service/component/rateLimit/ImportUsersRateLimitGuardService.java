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
 * - Enforces cooldown after jobs limit is reached.
 * Why Needed:
 * - Protects DB + CPU from overload.
 * - Ensures multi-tenant fairness.
 * Design:
 * - Redis based
 * - Using Lua Script for atomicity
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
    public void checkIfAllowed(String userId, long count, String jobId){
        ImportJobCostTier tier = ImportJobCostTier.fromCount(count);

        log.info("[RATE_LIMIT] Checking request | userId={} tier={} jobId={} requestedCount={}",
                userId, tier, jobId, count);

        checkCooldown(userId);

        /*//COMMENTED: cooldown state moved from JVM memory to Redis
        UserRateState state = getOrCreateState(userId);
        LocalDateTime allowedAt = state.getNextImportAllowedAt();
        if(allowedAt != null && LocalDateTime.now().isBefore(allowedAt)){
            throw new TooManyRequestsException(
                    "You must wait until " + allowedAt + " before starting another import."
            );
        }AtomicInteger running = state.getRunningJobs().computeIfAbsent(tier, t -> new AtomicInteger(0));*/

        String userRunningJobKey = RedisKeysGenerator.userRunningJobsKey(userId, tier);
        int limit = tier.getMaxConcurrentJobs();
        boolean allowed = redisStore.executeLuaScriptToCheckIfAllowed(userRunningJobKey, limit, jobId);

        if(!allowed){
            log.warn("[RATE_LIMIT] BLOCKED | userId={} tier={} jobId={} reason=CONCURRENCY_LIMIT maxAllowed={}",userId, tier, jobId, limit);
            throw new TooManyRequestsException(tier.name() + " concurrency limit reached. Max allowed = "+ tier.getMaxConcurrentJobs());
        }
            log.info("[RATE_LIMIT] ALLOWED | userId={} tier={} jobId={} slotReserved=true",userId, tier, jobId);
        // Called after Lua allows the request to create the TTL safety key and enable automatic cleanup if the job never finishes
        markJobStarted(userId, jobId, tier);

        // Tier concurrency check --- Logic shifted to Lua Script for Atomicity
        // Uses Redis SET size for fast, non-blocking per-user per-tier concurrency check
        /*long running = redisStore.countPerUserRunningJobsFromSet(userRunningJobKey);
        if(running >= tier.getMaxConcurrentJobs()){
            String cooldownKey = RedisKeysGenerator.cooldownKey(userId);
            // This TTL represents how long the USER must wait after hitting a concurrency limit.
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
        }*/
    }



    /**
     * MUST be called once job is ACCEPTED.
     * Uses atomic increment → thread safe.
     */
    public void markJobStarted(String userId, String jobId, ImportJobCostTier tier){

        String jobKey = RedisKeysGenerator.jobKey(userId,tier,jobId);
        // This TTL is about FAILURE SAFETY, not rate limiting.If the app crashes or never calls markJobFinished(), Redis will auto-cleanup this job key
        redisStore.setWithTtl(jobKey, tier.name(), Duration.ofMinutes(15));

        String userRunningJobKey = RedisKeysGenerator.userRunningJobsKey(userId, tier);
        // Add jobId to Redis SET to maintain fast and accurate per-user per-tier concurrency tracking
        //We commented the below line because Lua Script will add the Key and Value for the Redis SET if Allowed.
        //redisStore.insertUserRunningJobsInsideSet(userRunningJobKey, jobId);

        long running = redisStore.countPerUserRunningJobsFromSet(userRunningJobKey);
        log.info("[RATE_LIMIT] Mark Job started | userId={} tier={} jobId={} runningJobs={}",userId,tier,jobId,running);
    }



    /**
     * MUST be called in ASYNC finally block.
     * Handles:
     * decrement
     * XL cooldown
     */
    public void markJobFinished(String userId, String jobId, ImportJobCostTier tier){

        String jobKey = RedisKeysGenerator.jobKey(userId, tier, jobId);
        // Remove the TTL safety key — marks job as no longer active in Redis
        redisStore.delete(jobKey);
        String runningJobsKey = RedisKeysGenerator.userRunningJobsKey(userId, tier);
        // Remove jobId from running jobs SET — keeps concurrency count accurate
        redisStore.deleteUserRunningJobsFromSet(runningJobsKey, jobId);
        log.info(
                "[RATE_LIMIT] Job finished | userId={} tier={} jobId={} jobKeyDeleted={} setUpdated={}",userId,tier,jobId,jobKey,runningJobsKey);
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

