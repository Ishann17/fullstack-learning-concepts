package com.ishan.user_service.service.ratelimit;

/**
 * Defines cost tiers for import jobs based on requested user count.
 * Purpose:
 * - Helps rate limiting logic decide how "expensive" a job is.
 * - Higher tier = stricter concurrency limits + cooldown rules.
 * Why Enum?
 * - Central place to manage tier rules.
 * - Avoids hardcoding count checks across multiple classes.
 */
public enum ImportJobCostTier {

    // Very small jobs → Allow high concurrency
    SMALL,

    // Moderate jobs → Limited concurrency
    MEDIUM,

    // Heavy jobs → Strict concurrency
    LARGE,

    // Extremely heavy jobs → Usually only 1 at a time + cooldown
    XL;

    /**
     * Converts requested import count into a cost tier.
     * Used By:
     * - RateLimitGuardService to decide if job should be allowed.
     * Rule:
     * Smaller count → Lower tier → More jobs allowed
     * Larger count → Higher tier → Fewer jobs allowed
     */
    public static ImportJobCostTier fromCount(long count){

        // Small job → Safe to allow many parallel executions
        if(count <= 10) return SMALL;

        // Medium job → Controlled parallel execution
        if(count <= 1000) return MEDIUM;

        // Large job → Very limited parallel execution
        if(count <= 100000) return LARGE;

        // Anything bigger → Treated as extreme load
        return XL;
    }
}
