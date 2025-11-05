package com.campus.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存，如果不存在则通过loader加载
     */
    public <T> T getOrLoad(String key, Class<T> clazz, CacheLoader<T> loader, long timeout, TimeUnit unit) {
        try {
            // 尝试从缓存获取
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return clazz.cast(value);
            }

            // 获取分布式锁
            String lockKey = "lock:" + key;
            boolean locked = tryLock(lockKey, 10, TimeUnit.SECONDS);
            
            if (locked) {
                try {
                    // 双重检查
                    value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        return clazz.cast(value);
                    }

                    // 加载数据
                    T result = loader.load();
                    if (result != null) {
                        redisTemplate.opsForValue().set(key, result, timeout, unit);
                    } else {
                        // 缓存空值防止缓存穿透
                        redisTemplate.opsForValue().set(key, new Object(), 5, TimeUnit.MINUTES);
                    }
                    return result;
                } finally {
                    releaseLock(lockKey);
                }
            } else {
                // 未获取到锁，等待后重试
                Thread.sleep(100);
                return getOrLoad(key, clazz, loader, timeout, unit);
            }
        } catch (Exception e) {
            log.error("缓存获取异常 key: {}", key, e);
            return loader.load(); // 降级到直接加载
        }
    }

    /**
     * 尝试获取分布式锁
     */
    private boolean tryLock(String key, long timeout, TimeUnit unit) {
        String luaScript = 
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
            "   return redis.call('expire', KEYS[1], ARGV[2]) " +
            "else " +
            "   return 0 " +
            "end";
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);
        
        List<String> keys = Arrays.asList(key);
        Long result = redisTemplate.execute(script, keys, "1", String.valueOf(unit.toSeconds(timeout)));
        
        return result != null && result == 1;
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    @FunctionalInterface
    public interface CacheLoader<T> {
        T load();
    }
}
