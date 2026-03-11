package com.ishan.user_service.component.rateLimit;


import com.ishan.user_service.component.redis.RedisStore;
import com.ishan.user_service.customExceptions.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enables Mockito in JUnit 5 so that @Mock and @InjectMocks work automatically
public class ImportUsersRateLimitGuardServiceTest {

    @Mock // Creates a fake RedisStore so tests run fast and without real Redis
    private RedisStore redisStore;

    @InjectMocks // Creates the real service and automatically injects the mocked dependencies into it
    private ImportUsersRateLimitGuardService rateLimitGuardService;

    @Test
    void shouldAllowJob_whenLuaReturnsTrue() {

        String userId = "ishan";
        long count = 50;
        String jobId = "job-123";

        /* Your mock (redisStore) is a dummy object that does nothing until you tell it what to do
       Mockito has three core operations:
       1. when(...) → teach the mock how to behave
       2. thenReturn(...) → tech the mock what value to return*/
        // teach the mock how Redis should behave
        when(redisStore.executeLuaScriptToCheckIfAllowed(anyString(), anyInt(), eq(jobId)))
                .thenReturn(true);

        // call the actual method
        rateLimitGuardService.checkIfAllowed(userId, count, jobId);

        // verify Redis was consulted
        verify(redisStore).executeLuaScriptToCheckIfAllowed(anyString(), anyInt(), eq(jobId));

        // verify job TTL key was created
        verify(redisStore).setWithTtl(anyString(), anyString(), any(Duration.class));

    }

    @Test
    void shouldThrowException_whenConcurrencyLimitReached() {

        //Arrange
        String userId = "ishan";
        long count = 50;
        String jobId = "job-123";


        //tell mock what to do
        when(redisStore.executeLuaScriptToCheckIfAllowed(anyString(), anyInt(), eq(jobId))).thenReturn(false);

        assertThrows(TooManyRequestsException.class, () -> rateLimitGuardService.checkIfAllowed(userId, count, jobId));

        //verify
        verify(redisStore,never()).setWithTtl(anyString(), anyString(), any(Duration.class));
    }
}
