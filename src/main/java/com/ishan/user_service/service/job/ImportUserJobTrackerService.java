package com.ishan.user_service.service.job;

import com.ishan.user_service.dto.ImportUserJobInfoDto;

public interface ImportUserJobTrackerService {

    // Creates a new job entry and returns a unique jobId (like an orderId)
    String createJob(String userId, int requestedCount);

    // Fetch current job status (so client can poll)
    ImportUserJobInfoDto getJobStatus(String jobId);

    // Mark job as RUNNING
    void markRunning(String jobId);

    // Update processed count during execution
    void updateProgress(String jobId, long processedCount);

    // Mark job as SUCCESS
    void markCompleted(String jobId, String message);

    // Mark job as FAILED (and store error message)
    void markFailed(String jobId, String errorMessage);
}
