package com.shuttle.shuttlesystem.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.routes.ttl:600000}")
    private long routesTtl;

    @Value("${cache.stops.ttl:900000}")
    private long stopsTtl;

    @Value("${cache.shuttles.ttl:300000}")
    private long shuttlesTtl;

    @Value("${cache.student-stats.ttl:180000}")
    private long studentStatsTtl;

    @Value("${cache.analytics.ttl:300000}")
    private long analyticsTtl;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Configure different TTL for different cache names
        cacheConfigurations.put("routes", createCacheConfiguration(Duration.ofMillis(routesTtl)));
        cacheConfigurations.put("stops", createCacheConfiguration(Duration.ofMillis(stopsTtl)));
        cacheConfigurations.put("shuttles", createCacheConfiguration(Duration.ofMillis(shuttlesTtl)));
        cacheConfigurations.put("student-stats", createCacheConfiguration(Duration.ofMillis(studentStatsTtl)));
        cacheConfigurations.put("analytics", createCacheConfiguration(Duration.ofMillis(analyticsTtl)));
        cacheConfigurations.put("bookings", createCacheConfiguration(Duration.ofMillis(300000))); // 5 minutes
        cacheConfigurations.put("wallet", createCacheConfiguration(Duration.ofMillis(180000))); // 3 minutes
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(createCacheConfiguration(Duration.ofMillis(300000))) // Default 5 minutes
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
} 