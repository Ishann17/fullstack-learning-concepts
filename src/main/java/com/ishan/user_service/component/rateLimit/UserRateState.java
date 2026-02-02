package com.ishan.user_service.component.rateLimit;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores runtime rate-limit state for a single user.
 * Purpose:
 * - Tracks how many jobs user is currently running per cost tier.
 * - Helps enforce concurrency limits.
 * - Tracks cooldown after heavy (XL) jobs.
 * Why Needed:
 * - Rate limiting must be per-user (multi-tenant fairness).
 * - Prevents one user from overloading system resources.
 * Used By:
 * - ImportUsersRateLimitGuardService
 */
@Getter
@Setter
public class UserRateState {

    /**
     * Tracks number of currently running jobs per tier.
     * Why AtomicInteger?
     * - Async jobs run on multiple threads.
     * - Prevents race conditions without heavy locking.
     * Why pre-initialize?
     * - Avoids NullPointerExceptions
     * - Simplifies guard logic
     */
    private final Map<ImportJobCostTier, AtomicInteger> runningJobs = new ConcurrentHashMap<>();

    /**
     * Global cooldown timestamp.
     * If user triggers an XL job:
     * → block ALL future jobs until this time.
     */
    private volatile LocalDateTime nextImportAllowedAt;

    public UserRateState() {
        for (ImportJobCostTier tier : ImportJobCostTier.values()) {
            runningJobs.put(tier, new AtomicInteger(0));
        }
    }
}

