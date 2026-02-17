package com.ishan.user_service.component.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

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

    /**
     * Registers the RedisMessageListenerContainer responsible for listening to
     * Redis key expiry events and routing them to our application listener.
     *
     * PURPOSE:
     * - Subscribes our application to Redis key expiration notifications.
     * - Enables automatic cleanup of stale job entries from the running jobs SET.
     * - Acts as the bridge between Redis Pub/Sub and Spring listener code.
     *
     * WHY WE NEED THIS:
     * - When a job crashes, its TTL safety key eventually expires.
     * - Redis publishes an expiry event via Pub/Sub.
     * - Without this container, our RedisKeyExpiryListener would never receive events.
     * - This is what makes the rate limiter self-healing.
     *
     * HOW IT WORKS (FLOW):
     * Redis key expires
     * → Redis publishes "__keyevent@0__:expired"
     * → RedisMessageListenerContainer receives the event
     * → RedisKeyExpiryListener.onMessage() is invoked
     * → Stale jobId is removed from the running SET
     *
     * CONNECTION FACTORY — WHY REQUIRED:
     * - Provides the physical connection to Redis.
     * - The container uses it to open a dedicated Pub/Sub connection.
     * - This is different from normal Redis commands (it is a long-lived subscription).
     * - Without setting the connection factory, the listener cannot communicate with Redis.
     *
     * PATTERN TOPIC — WHY USED:
     * - Redis sends expiry events on a special channel:
     *   "__keyevent@0__:expired"
     * - We subscribe to this channel so our app is notified whenever any key expires.
     * - PatternTopic tells Spring to use Redis pattern subscription (P_SUBSCRIBE)
     *   instead of normal subscription.
     * SIMPLE MEANING:
     * - Without this, our listener would never hear about expired keys.
     * - With this in place, Redis actively notifies our application.
     *
     * DESIGN LEARNING:
     * - Pub/Sub listeners require a long-running container managed by Spring.
     * - Key expiry notifications are best-effort, not guaranteed delivery.
     * - This listener provides fast cleanup but may later be complemented by a periodic sweeper.
     *
     * RESULT:
     * - Stale jobs are automatically cleaned after TTL expiry.
     * - Concurrency slots are eventually recovered.
     * - System becomes resilient to crashes and orphaned state.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory, RedisKeyExpiryListener expiryListener){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                expiryListener,
                new PatternTopic("__keyevent@0__:expired")
        );
        return container;
    }
}
