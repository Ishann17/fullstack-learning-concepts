package com.ishan.user_service.component.rateLimit;


import com.ishan.user_service.component.redis.RedisStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Enables Mockito in JUnit 5 so that @Mock and @InjectMocks work automatically
public class ImportUsersRateLimitGuardServiceTest {

    @Mock // Creates a fake RedisStore so tests run fast and without real Redis
    private RedisStore redisStore;

    @InjectMocks // Creates the real service and automatically injects the mocked dependencies into it
    private ImportUsersRateLimitGuardService rateLimitGuardService;

    @Test
    void shouldAllowJob_whenLuaReturnsTrue() {

        // Arrange
        String userId = "ishan";
        long count = 15;
        String jobId = "job-123";


       /* Your mock (redisStore) is a dummy object that does nothing until you tell it what to do
       Mockito has three core operations:
       1. when(...) → teach the mock how to behave
       2. thenReturn(...) → what value to return
       3. verify(...) → check what interactions happened */

        when(redisStore.executeLuaScriptToCheckIfAllowed(anyString(), anyInt(), eq(jobId))).thenReturn(true);

        // Act
        rateLimitGuardService.checkIfAllowed(userId, count, jobId);

        // Assert
        verify(redisStore).setWithTtl(
                anyString(),
                anyString(),
                any()
        );
    }
}
