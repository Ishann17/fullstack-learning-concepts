package com.ishan.user_service.service.ratelimit;

import com.ishan.user_service.customExceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Stores per-user runtime rate limit state.
     * Key = userId
     * Value = UserRateState snapshot
     */
    private final Map<String, UserRateState> userStateMap = new ConcurrentHashMap<>();

    /**
     * Validates if user is allowed to start a new job.
     *
     * Flow:
     * 1. Resolve tier from count
     * 2. Load user rate state
     * 3. Check concurrency limits
     * 4. Check cooldown rules
     *
     * Throws:
     * - TooManyRequestsException
     */
    public void checkIfAllowed(String userId, long count) {

        ImportJobCostTier tier = ImportJobCostTier.fromCount(count);

        UserRateState state = getOrCreateState(userId);

        //  concurrency + cooldown checks here

        switch(tier){

            case SMALL -> {
                if(state.getRunningSmallJobs() >= 10){
                    throw new TooManyRequestsException("SMALL job limit reached (max 10 concurrent)");
                }
            }

            case MEDIUM -> {
                if(state.getRunningMediumJobs() >= 5){
                    throw new TooManyRequestsException("Medium job limit reached (max 5 concurrent)");
                }
            }

            case LARGE -> {
                if(state.getRunningLargeJobs() >= 3){
                    throw new TooManyRequestsException("Large job limit reached (max 3 concurrent)");
                }
            }

            case XL -> {
                if(state.getRunningXLJobs() >= 1){
                    throw new TooManyRequestsException("XL job limit reached (max 1 concurrent)");
                }

                //Set cool down for next 10 seconds
                LocalDateTime lastFinish = state.getLastXLJobFinishedAt();
                if(lastFinish != null){
                    LocalDateTime allowedAfter = lastFinish.plusSeconds(10);

                    if(LocalDateTime.now().isBefore(allowedAfter)){
                        throw new TooManyRequestsException("You have wait for next 10 seconds in order to make another XL Job run");
                    }
                }
            }
        }
    }

    /**
     * Must be called AFTER job is accepted and async execution starts.
     *
     * Purpose:
     * - Increment running job counters for the tier.
     */
    public void markJobStarted(String userId, ImportJobCostTier tier) {

        UserRateState state = getOrCreateState(userId);

        switch (tier) {
            case SMALL -> state.setRunningSmallJobs(state.getRunningSmallJobs() + 1);
            case MEDIUM -> state.setRunningMediumJobs(state.getRunningMediumJobs() + 1);
            case LARGE -> state.setRunningLargeJobs(state.getRunningLargeJobs() + 1);
            case XL -> state.setRunningXLJobs(state.getRunningXLJobs() + 1);
        }
    }


    /**
     * Must be called AFTER async job completes (success or failure).
     *
     * Purpose:
     * - Decrement running job counters.
     * - Record XL cooldown timestamp.
     */
    public void markJobFinished(String userId, ImportJobCostTier tier) {

        UserRateState state = getOrCreateState(userId);

        switch (tier) {
            case SMALL -> state.setRunningSmallJobs(state.getRunningSmallJobs() - 1);
            case MEDIUM -> state.setRunningMediumJobs(state.getRunningMediumJobs() - 1);
            case LARGE -> state.setRunningLargeJobs(state.getRunningLargeJobs() - 1);
            case XL -> {
                state.setRunningXLJobs(state.getRunningXLJobs() - 1);
                state.setLastXLJobFinishedAt(LocalDateTime.now());
            }
        }
    }


    /* =========================================================
       INTERNAL HELPERS
       ========================================================= */

    /**
     * Returns existing state OR creates new state for user.
     */
    private UserRateState getOrCreateState(String userId) {
        return userStateMap.computeIfAbsent(userId, id -> new UserRateState());
    }
}
