package com.ishan.user_service.component.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisConfig {



    /**
     * Registers the Lua script used for atomic rate-limit checks.
     *
     * PURPOSE:
     * - Loads the Lua script from the classpath so Spring can execute it via Redis.
     * - This script performs the "check limit + add job" step in ONE atomic operation.
     *
     * WHY WE NEED THIS:
     * - Multiple requests may hit at the same time.
     * - If we check and add separately in Java, race conditions can occur.
     * - Lua runs inside Redis as a single uninterrupted operation → guarantees correctness.
     *
     * DESIGN LEARNING:
     * - Spring needs this bean to know which script to execute.
     * - Script is stored under: redis/check_and_add_running_job.lua
     * - Result type is Long because Lua returns 1 (allowed) or 0 (blocked).
     */
    @Bean
    public DefaultRedisScript<Long> checkAndAddRunningJobScript(){
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/check_and_add_running_job.lua"));
        script.setResultType(Long.class);
        return script;
    }
}
