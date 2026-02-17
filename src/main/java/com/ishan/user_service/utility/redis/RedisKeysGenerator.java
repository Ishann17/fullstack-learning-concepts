package com.ishan.user_service.utility.redis;

import com.ishan.user_service.component.rateLimit.ImportJobCostTier;

/**
 * Central place for all Redis key formats used in the system.
 *
 * WHY THIS EXISTS:
 * - Redis keys are plain strings → easy to typo, hard to refactor
 * - This class provides ONE source of truth for key structure
 * - Makes Redis debugging, refactoring, and reasoning predictable
 *
 * DESIGN LEARNING:
 * - Redis stores STATE, not rules
 * - Keys must encode intent clearly (user, job, cooldown, etc.)
 * - Never scatter string keys across business logic
 */
public final class RedisKeysGenerator {

    /**
     * Utility class → should never be instantiated.
     * Private constructor enforces correct usage.
     */
    private RedisKeysGenerator() {}

    /**
     * Redis key representing a single running job (job lease).
     *
     * PURPOSE:
     * - Each running job is represented by exactly one Redis key
     * - Presence of this key == job is currently running
     *
     * WHY THIS DESIGN:
     * - We do NOT store counters (they break on crashes)
     * - Concurrency is derived by COUNTING these keys
     * - Redis TTL on this key guarantees automatic cleanup
     *
     * FAILURE SAFETY:
     * - If the application crashes or markJobFinished() is never called,
     *   Redis automatically deletes the key when TTL expires
     * - This prevents permanent permit leaks and self-heals the system
     *
     * LEARNING:
     * - Job is the unit of truth, not user state
     * - Derived state (counts) should be computed, not stored
     */
    public static String jobKey(String userId, ImportJobCostTier tier, String jobId) {
        return "job:"+userId+":"+tier.name()+":"+jobId;
    }

    /**
     * Key representing cooldown state after XL job completion.
     *
     * Example:
     * user:vasu:cooldown
     *
     * PURPOSE:
     * - Enforces "wait period" after all jobs as per tier cool down seconds
     * - TTL-based → automatically expires
     * - Must be visible across all pods
     */
    public static String cooldownKey(String userId) {
        return "user:" + userId + ":cooldown";
    }


    public static String userRunningJobsKey(String userId, ImportJobCostTier tier) {
        return "user:" + userId + ":" + tier.name() + ":jobs";
    }


}
