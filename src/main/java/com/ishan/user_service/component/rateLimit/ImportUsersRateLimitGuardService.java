package com.ishan.user_service.component.rateLimit;

import com.ishan.user_service.customExceptions.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final Map<String, UserRateState> userStateMap = new ConcurrentHashMap<>();


    /**
     * MAIN ENTRY — decides if a job is allowed.
     *
     * Order matters:
     * Check GLOBAL cooldown (XL protection)
     * Check tier concurrency
     */
    public void checkIfAllowed(String userId, long count){

        ImportJobCostTier tier = ImportJobCostTier.fromCount(count);
        UserRateState state = getOrCreateState(userId);

        //GLOBAL cooldown check
        LocalDateTime allowedAt = state.getNextImportAllowedAt();

        if(allowedAt != null && LocalDateTime.now().isBefore(allowedAt)){
            throw new TooManyRequestsException(
                    "You must wait until " + allowedAt + " before starting another import."
            );
        }

        // ✅ Tier concurrency check
        AtomicInteger running =
                state.getRunningJobs()
                        .computeIfAbsent(tier, t -> new AtomicInteger(0));

        if(running.get() >= tier.getMaxConcurrentJobs()){
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
    public void markJobStarted(String userId, ImportJobCostTier tier){

        UserRateState state = getOrCreateState(userId);
        log.info("[RATE LIMIT GUARD]|User={} Tier={} Running={}",
                userId,
                tier,
                state.getRunningJobs().get(tier).get());


        state.getRunningJobs()
                .computeIfAbsent(tier, t -> new AtomicInteger(0))
                .incrementAndGet();
    }



    /**
     * MUST be called in ASYNC finally block.
     *
     * Handles:
     * ✅ decrement
     * ✅ XL cooldown
     */
    public void markJobFinished(String userId, ImportJobCostTier tier){

        UserRateState state = getOrCreateState(userId);

        AtomicInteger running =
                state.getRunningJobs().get(tier);

        if(running != null){
            running.decrementAndGet();
        }

        // 🔥 GLOBAL cooldown triggered ONLY by XL
        if(tier == ImportJobCostTier.XL){

            state.setNextImportAllowedAt(
                    LocalDateTime.now()
                            .plusSeconds(tier.getCooldownSeconds())
            );
        }
    }



    private UserRateState getOrCreateState(String userId){
        return userStateMap.computeIfAbsent(userId, id -> new UserRateState());
    }
}

