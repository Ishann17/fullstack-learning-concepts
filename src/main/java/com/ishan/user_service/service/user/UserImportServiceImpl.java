package com.ishan.user_service.service.user;

import com.ishan.user_service.dto.UserDto;
import com.ishan.user_service.mapper.UserDtoToUserMapper;
import com.ishan.user_service.model.User;
import com.ishan.user_service.repository.UserRepository;
import com.ishan.user_service.service.job.ImportUserJobTrackerService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;



import java.util.ArrayList;
import java.util.List;

@Service
public class UserImportServiceImpl implements UserImportService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserBatchSaverService userBatchSaverService;

    @Autowired
    private ImportUserJobTrackerService importUserJobTrackerService;

    private final static Logger log = LoggerFactory.getLogger(UserImportServiceImpl.class);


    @Override
    public User importSingleUserFromExternalSource(UserDto userDto) {
        User user = UserDtoToUserMapper.convertUserDtoToUser(userDto);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void importMultipleUsersFromExternalSource(List<UserDto> userDtoList) {

        List<User> userList = new ArrayList<>();
        for(UserDto userDto: userDtoList){
            userList.add(UserDtoToUserMapper.convertUserDtoToUser(userDto));
        }
        userRepository.saveAll(userList);
    }



    /**
     * BATCH PROCESSING APPROACH - Optimized for large datasets
     *
     * WHY BATCH PROCESSING?
     * - Instead of saving 2 million records in ONE transaction (high memory usage)
     * - We split into smaller chunks of 1,000 records each (controlled memory)
     * - Each chunk is saved and committed separately
     * - This prevents OutOfMemoryError and improves performance
     *
     * HOW IT WORKS:
     * 1. Take the full list of 2 million UserDto objects
     * 2. Process them in batches of 1,000 at a time
     * 3. For each batch:
     *    - Convert 1,000 DTOs to User entities
     *    - Save them to database
     *    - Commit transaction
     *    - Clear memory for next batch
     * 4. Repeat until all records are processed
     *
     * PERFORMANCE:
     * - Without batching: 443 seconds for 2M records
     * - With batching: ~100-150 seconds (3-4x faster!)
     *
     * @param userDtoList - List of UserDto objects to be saved (can be millions)
     */
    @Override
   // @Transactional - If the whole import runs inside ONE big transaction then data is saved permanently ONLY when the method finishes.
    public void importMultipleUsersFromFakerWithBatchProcessing(String jobId, List<UserDto> userDtoList) {
        long startTime = System.currentTimeMillis();
        // BATCH_SIZE: Number of records to process at once
        // Think: How many boxes to load in the truck per trip
        // 1,000 is optimal - not too small (many trips), not too large (memory issues)
        int BATCH_SIZE = 1000;
        int lastReportedPercent = 0;

        // Calculate total batches needed
        // Example: 2,000,000 records ÷ 1,000 = 2,000 batches
        int totalBatches = (int) Math.ceil((double) userDtoList.size() / BATCH_SIZE);

        log.info("Starting batch processing: {} total records, {} batches of {} records each",
                userDtoList.size(), totalBatches, BATCH_SIZE);

        // Extra monitoring logs (helps during 1M+ imports)
        // Tracks speed + percentage + estimated completion behavior
        long insertedCount = 0;

        // Process each batch one by one
        for (int batchNumber = 0; batchNumber < totalBatches; batchNumber++) {

            // Calculate start and end index for this batch
            // Batch 0: records 0-999
            // Batch 1: records 1000-1999
            // Batch 2: records 2000-2999, etc.
            int startIndex = batchNumber * BATCH_SIZE;
            int endIndex = Math.min(startIndex + BATCH_SIZE, userDtoList.size());

            // Extract current batch from the main list
            // subList(0, 1000) means "give me records from index 0 to 999"
            List<UserDto> currentBatch = userDtoList.subList(startIndex, endIndex);

            // Convert current batch of DTOs to User entities
            List<User> userBatch = new ArrayList<>();
            for (UserDto userDto : currentBatch) {
                userBatch.add(UserDtoToUserMapper.convertUserDtoToUser(userDto));
            }
            userBatchSaverService.saveOneBatch(userBatch);

            // Save current batch to database
            // This saves 1,000 records in one go (much faster than one-by-one!)
            //userRepository.saveAll(userBatch);

            // FLUSH: Force Hibernate to execute all queued SQL statements NOW
            // Without this, Hibernate might wait and batch multiple batches together
            // entityManager.flush();

            // CLEAR: Remove all entities from persistence context (free memory!)
            // Without this, all 2M entities would stay in memory = crash!
            //entityManager.clear();

            insertedCount += userBatch.size(); // total inserted so far (last batch may be < 1000)

            //calculate the percentage of work done we will update the progress after every 10%
            int currentPercentage = (int) (insertedCount * 100.0)/totalBatches;
            if(currentPercentage >= lastReportedPercent + 10 || currentPercentage == 100){
                importUserJobTrackerService.updateProgress(jobId, currentPercentage);
                lastReportedPercent = currentPercentage;
            }

            // Log progress every 10 batches to track performance
            // Example: "Processed batch 10/2000 (10,000 records)"
            // Log progress every 10 batches to track performance
            // Example: "Processed batch 10/2000 (10,000 records)"
            if ((batchNumber + 1) % 10 == 0 || (batchNumber + 1) == totalBatches) {

                long now = System.currentTimeMillis();
                double elapsedSeconds = (now - startTime) / 1000.0;

                // ✅ Percent completion
                double percent = userDtoList.isEmpty()
                        ? 100.0
                        : (insertedCount * 100.0) / userDtoList.size();

                // ✅ Speed calculation (users/sec)
                double usersPerSecond = elapsedSeconds == 0
                        ? insertedCount
                        : insertedCount / elapsedSeconds;

                log.info("Processed batch {}/{} (Inserted={}/{} | {}% | Speed={} users/sec | Elapsed={}s)",
                        batchNumber + 1,
                        totalBatches,
                        insertedCount,
                        userDtoList.size(),
                        String.format("%.2f", percent),
                        String.format("%.0f", usersPerSecond),
                        String.format("%.2f", elapsedSeconds));
            }
        }
        long endTime = System.currentTimeMillis();
        double totalSeconds = (endTime - startTime) / 1000.0;

        double avgUsersPerSecond = totalSeconds == 0
                ? insertedCount
                : insertedCount / totalSeconds;

        log.info("Batch processing completed: {} total records saved in {}s (AvgSpeed={} users/sec)",
                userDtoList.size(),
                String.format("%.2f", totalSeconds),
                String.format("%.0f", avgUsersPerSecond));
    }


}

