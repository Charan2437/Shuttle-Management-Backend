package com.shuttle.shuttlesystem.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Get value from cache
     */
    public Object getFromCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            return wrapper != null ? wrapper.get() : null;
        }
        return null;
    }

    /**
     * Put value in cache
     */
    public void putInCache(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * Evict specific key from cache
     */
    public void evictFromCache(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    /**
     * Clear entire cache
     */
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Set value with custom TTL using Redis directly
     */
    public void setWithTTL(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Get value using Redis directly
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Delete key using Redis directly
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Check if key exists
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Invalidate all caches related to routes
     */
    public void invalidateRouteCaches() {
        clearCache("routes");
        clearCache("analytics");
    }

    /**
     * Invalidate all caches related to bookings
     */
    public void invalidateBookingCaches() {
        clearCache("bookings");
        clearCache("analytics");
        clearCache("student-stats");
    }

    /**
     * Invalidate all caches related to wallet
     */
    public void invalidateWalletCaches() {
        clearCache("wallet");
        clearCache("student-stats");
    }

    /**
     * Invalidate all caches related to shuttles
     */
    public void invalidateShuttleCaches() {
        clearCache("shuttles");
        clearCache("analytics");
    }
} 