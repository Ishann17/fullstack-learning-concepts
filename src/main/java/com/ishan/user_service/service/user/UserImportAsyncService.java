package com.ishan.user_service.service.user;


import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.service.MockUserGeneratorService;
import com.ishan.user_service.service.job.ImportUserJobTrackerService;
import com.ishan.user_service.component.rateLimit.ImportJobCostTier;
import com.ishan.user_service.component.rateLimit.ImportUsersRateLimitGuardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/*@Async works only when the method is called from another Spring bean.
 Self-calls bypass Spring and run synchronously.*/
@Service
public class UserImportAsyncService {
    private static final Logger log = LoggerFactory.getLogger(UserImportAsyncService.class);

    private final UserImportService userImportService;
    private final MockUserGeneratorService mockUserGeneratorService;
    private final ImportUserJobTrackerService importUserJobTrackerService;
    private final ImportUsersRateLimitGuardService rateLimitGuardService;

    public UserImportAsyncService(UserImportService userImportService, MockUserGeneratorService mockUserGeneratorService, ImportUserJobTrackerService importUserJobTrackerService, ImportUsersRateLimitGuardService rateLimitGuardService) {
        this.userImportService = userImportService;
        this.mockUserGeneratorService = mockUserGeneratorService;
        this.importUserJobTrackerService = importUserJobTrackerService;
        this.rateLimitGuardService = rateLimitGuardService;
    }

    @Async
    //@Async method must be public and called from another Spring bean.
    public void runFakerImportAsync(String userId, String jobId, int count, ImportJobCostTier tier){
        long startTime = System.currentTimeMillis();

        log.info("[Async] Import started | jobId={} requestedCount={}", jobId, count);

        try{
            List<UserDto> userDtoList = mockUserGeneratorService.generateUsers(count);
            log.info("[Async] Faker generation done | jobId={} generated={}", jobId, userDtoList.size());

            importUserJobTrackerService.markRunning(jobId);

            userImportService.importMultipleUsersFromFakerWithBatchProcessing(jobId,userDtoList);
            importUserJobTrackerService.markCompleted(jobId, "Imported " + userDtoList.size() + " users successfully");
        }catch (Exception ex){
            importUserJobTrackerService.markFailed(jobId, ex.getMessage());
            log.error("[Async] Import failed | jobId={}", jobId, ex);
        }finally {
            rateLimitGuardService.markJobFinished(userId,tier);
            double secs = (System.currentTimeMillis() - startTime) / 1000.0;
            log.info("[Async] Import finished | jobId={} timeTakenSec={}",
                    jobId, String.format("%.2f", secs));
        }

    }

}
