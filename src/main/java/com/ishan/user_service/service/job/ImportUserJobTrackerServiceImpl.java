package com.ishan.user_service.service.job;

import com.ishan.user_service.dto.ImportUserJobInfoDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImportUserJobTrackerServiceImpl implements ImportUserJobTrackerService{

    //We need to store Job info inside memory for this we will use Map
    private final Map<String, ImportUserJobInfoDto> jobInfoDtoMap = new ConcurrentHashMap<>();

    @Override
    public String createJob(String userId, int requestedCount) {
        String jobId = UUID.randomUUID().toString();
        ImportUserJobInfoDto jobInfo = ImportUserJobInfoDto.builder()
                                        .jobId(jobId)
                                        .status(ImportUserJobInfoDto.ImportStatus.PENDING)
                                        .requestedCount(requestedCount)
                                        .processedCount(0)
                                        .message("User Import Job Created")
                                        .startedAt(LocalDateTime.now())
                                        .build();
        jobInfoDtoMap.put(jobId, jobInfo);
        return jobId;
    }

    @Override
    public ImportUserJobInfoDto getJobStatus(String jobId) {
        return jobInfoDtoMap.get(jobId);
    }

    @Override
    public void markRunning(String jobId) {
        ImportUserJobInfoDto jobInfoDto = jobInfoDtoMap.get(jobId);
        if(Objects.nonNull(jobInfoDto)){
            jobInfoDto.setStatus(ImportUserJobInfoDto.ImportStatus.IN_PROGRESS);
        }
    }

    @Override
    public void updateProgress(String jobId, long processedCount) {
        ImportUserJobInfoDto jobInfoDto = jobInfoDtoMap.get(jobId);
        if(Objects.nonNull(jobInfoDto)){
            jobInfoDto.setProcessedCount(processedCount);
        }
    }

    @Override
    public void markCompleted(String jobId, String message) {
        ImportUserJobInfoDto jobInfoDto = jobInfoDtoMap.get(jobId);
        if(Objects.nonNull(jobInfoDto)){
            jobInfoDto.setStatus(ImportUserJobInfoDto.ImportStatus.COMPLETED);
            jobInfoDto.setMessage(message);
            jobInfoDto.setFinishedAt(LocalDateTime.now());
        }
    }

    @Override
    public void markFailed(String jobId, String errorMessage) {
        ImportUserJobInfoDto jobInfoDto = jobInfoDtoMap.get(jobId);
        if(Objects.nonNull(jobInfoDto)){
            jobInfoDto.setStatus(ImportUserJobInfoDto.ImportStatus.FAILED);
            jobInfoDto.setMessage(errorMessage);
            jobInfoDto.setFinishedAt(LocalDateTime.now());
        }
    }
}
