package com.ishan.user_service.component;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthCheck {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisHealthCheck(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void checkRedisHealth(){
        stringRedisTemplate.opsForValue().set("redis-test","ok");
        String val = stringRedisTemplate.opsForValue().get("redis-test");
        System.out.println("Redis - Check = " + val);
    }
}
