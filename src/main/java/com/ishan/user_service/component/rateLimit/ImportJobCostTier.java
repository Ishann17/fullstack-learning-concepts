package com.ishan.user_service.component.rateLimit;

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
    SMALL(100,5,10),

    // Moderate jobs → Limited concurrency
    MEDIUM(10000, 10, 5),

    // Heavy jobs → Strict concurrency
    LARGE(100000, 20, 3),

    // Extremely heavy jobs → Usually only 1 at a time + cooldown
    XL(Long.MAX_VALUE,30,1);

    private final long maxCount;
    private final int cooldownSeconds;
    private final int maxConcurrentJobs;

    ImportJobCostTier(long maxCount, int cooldownSeconds, int maxConcurrentJobs){
        this.maxCount = maxCount;
        this.cooldownSeconds = cooldownSeconds;
        this.maxConcurrentJobs = maxConcurrentJobs;
    }

    public long getMaxCount() {
        return maxCount;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    /**
     * Converts requested import count into a cost tier.
     * Used By:
     * - RateLimitGuardService to decide if job should be allowed.
     * Rule:
     * Smaller count → Lower tier → More jobs allowed
     * Larger count → Higher tier → Fewer jobs allowed
     */
    public static ImportJobCostTier fromCount(long count){
        for (ImportJobCostTier tier : values()) {
            if (count <= tier.maxCount) {
                return tier;
            }
        }
        return XL;
    }
}
