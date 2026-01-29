package com.ishan.user_service.service.ratelimit;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores runtime rate-limit state for a single user.
 *
 * Purpose:
 * - Tracks how many jobs user is currently running per cost tier.
 * - Helps enforce concurrency limits.
 * - Tracks cooldown after heavy (XL) jobs.
 *
 * Why Needed:
 * - Rate limiting must be per-user (multi-tenant fairness).
 * - Prevents one user from overloading system resources.
 * Used By:
 * - ImportUsersRateLimitGuardService
 */
@Data
public class UserRateState {

    // Number of SMALL jobs currently running for this user
    private int runningSmallJobs;

    // Number of MEDIUM jobs currently running
    private int runningMediumJobs;

    // Number of LARGE jobs currently running
    private int runningLargeJobs;

    // Number of XL jobs currently running (usually 0 or 1)
    private int runningXLJobs;

    // Last time an XL job finished (used for cooldown logic)
    private LocalDateTime lastXLJobFinishedAt;

}
