# Caching Implementation for Shuttle System

## Overview

This document describes the comprehensive caching mechanism implemented for the Shuttle System APIs to improve performance and reduce database load.

## Architecture

The caching system uses **Redis** as the cache provider with **Spring Boot Cache** abstraction layer. The implementation includes:

- **Cache Configuration**: Custom TTL settings for different data types
- **Cache Annotations**: Spring's `@Cacheable` and `@CacheEvict` annotations
- **Cache Service**: Utility service for manual cache operations
- **Cache Management**: Admin endpoints for cache monitoring and invalidation

## Cache Configuration

### Dependencies Added

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Application Properties

```properties
# Cache Configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=300000
spring.cache.redis.cache-null-values=false
spring.cache.redis.use-key-prefix=true
spring.cache.redis.key-prefix=shuttle-system:

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.timeout=2000ms

# Cache-specific TTL configurations
cache.routes.ttl=600000      # 10 minutes
cache.stops.ttl=900000       # 15 minutes
cache.shuttles.ttl=300000    # 5 minutes
cache.student-stats.ttl=180000 # 3 minutes
cache.analytics.ttl=300000   # 5 minutes
```

## Cache Categories

### 1. Routes Cache (`routes`)
- **TTL**: 10 minutes
- **Cached Operations**:
  - `getAllRoutes()` - All routes with stops and hours
  - `getRouteById(UUID)` - Individual route details
- **Cache Keys**:
  - `'all'` - All routes
  - `{routeId}` - Individual route
- **Invalidation**: On route create/update/delete

### 2. Stops Cache (`stops`)
- **TTL**: 15 minutes
- **Cached Operations**:
  - `getAllStops()` - All stops
  - `getStopById(UUID)` - Individual stop
- **Cache Keys**:
  - `'all'` - All stops
  - `{stopId}` - Individual stop
- **Invalidation**: On stop create/update/delete

### 3. Shuttles Cache (`shuttles`)
- **TTL**: 5 minutes
- **Cached Operations**:
  - `getAllShuttles()` - All shuttles
  - `getShuttleById(UUID)` - Individual shuttle
  - `getAvailableShuttles()` - Available shuttles
  - `getAssignedShuttles()` - Assigned shuttles
- **Cache Keys**:
  - `'all'` - All shuttles
  - `{shuttleId}` - Individual shuttle
  - `'available'` - Available shuttles
  - `'assigned'` - Assigned shuttles
- **Invalidation**: On shuttle create/update/delete

### 4. Student Stats Cache (`student-stats`)
- **TTL**: 3 minutes
- **Cached Operations**:
  - `getStatsForStudent(String)` - Individual student stats
  - `getStatsForAllStudents()` - All students stats
- **Cache Keys**:
  - `{studentId}` - Individual student stats
  - `'all-students'` - All students stats
- **Invalidation**: On booking operations

### 5. Analytics Cache (`analytics`)
- **TTL**: 5 minutes
- **Cached Operations**:
  - `getOverview(String, String)` - System overview
  - `getRouteAnalytics(String, String)` - Route analytics
  - `getStudentAnalytics(String, String)` - Student analytics
- **Cache Keys**:
  - `'overview-{fromDate}-{toDate}'` - Overview analytics
  - `'route-analytics-{fromDate}-{toDate}'` - Route analytics
  - `'student-analytics-{fromDate}-{toDate}'` - Student analytics
- **Invalidation**: On booking operations

### 6. Bookings Cache (`bookings`)
- **TTL**: 5 minutes
- **Cached Operations**:
  - `getTripHistory()` - Student trip history
  - `getFrequentRoutes()` - Frequent routes
  - `getTravelAnalytics()` - Travel analytics
- **Cache Keys**:
  - `'trip-history-{email}-{limit}-{offset}-{fromDate}-{toDate}-{status}'`
  - `'frequent-routes-{email}-{limit}-{fromDate}-{toDate}'`
  - `'travel-analytics-{email}-{fromDate}-{toDate}'`
- **Invalidation**: On booking create/update/delete

## Implementation Details

### Cache Annotations Used

1. **@Cacheable**: Caches method results
   ```java
   @Cacheable(value = "routes", key = "'all'")
   public List<RouteWithStopsAndHoursDTO> getAllRoutes()
   ```

2. **@CacheEvict**: Removes cache entries
   ```java
   @CacheEvict(value = "routes", allEntries = true)
   public RouteWithStopsAndHoursDTO createRoute(RouteWithStopsAndHoursDTO dto)
   ```

### Cache Service Utility

The `CacheService` provides manual cache operations:

```java
@Service
public class CacheService {
    // Get value from cache
    public Object getFromCache(String cacheName, String key)
    
    // Put value in cache
    public void putInCache(String cacheName, String key, Object value)
    
    // Evict specific key
    public void evictFromCache(String cacheName, String key)
    
    // Clear entire cache
    public void clearCache(String cacheName)
    
    // Invalidate related caches
    public void invalidateRouteCaches()
    public void invalidateBookingCaches()
    public void invalidateWalletCaches()
    public void invalidateShuttleCaches()
}
```

## Cache Management API

### Admin Endpoints

All cache management endpoints are available under `/api/admin/cache/` and require ADMIN role:

1. **GET** `/api/admin/cache/status` - Check cache status
2. **DELETE** `/api/admin/cache/routes` - Clear routes cache
3. **DELETE** `/api/admin/cache/stops` - Clear stops cache
4. **DELETE** `/api/admin/cache/shuttles` - Clear shuttles cache
5. **DELETE** `/api/admin/cache/bookings` - Clear bookings cache
6. **DELETE** `/api/admin/cache/analytics` - Clear analytics cache
7. **DELETE** `/api/admin/cache/student-stats` - Clear student stats cache
8. **DELETE** `/api/admin/cache/wallet` - Clear wallet cache
9. **DELETE** `/api/admin/cache/all` - Clear all caches
10. **DELETE** `/api/admin/cache/{cacheName}` - Clear specific cache

## Performance Benefits

### Before Caching
- Database queries executed on every request
- High database load during peak usage
- Slower response times for frequently accessed data

### After Caching
- **Routes**: 10-minute cache reduces database queries by ~90%
- **Stops**: 15-minute cache for static data
- **Analytics**: 5-minute cache for expensive calculations
- **Student Stats**: 3-minute cache for frequently accessed data

### Expected Performance Improvements
- **Response Time**: 60-80% faster for cached data
- **Database Load**: 70-90% reduction in queries
- **Scalability**: Better handling of concurrent users
- **User Experience**: Faster page loads and smoother interactions

## Setup Instructions

### 1. Install Redis

**Windows:**
```bash
# Download Redis for Windows or use WSL
# Or use Docker
docker run -d -p 6379:6379 redis:latest
```

**macOS:**
```bash
brew install redis
brew services start redis
```

**Linux:**
```bash
sudo apt-get install redis-server
sudo systemctl start redis
```

### 2. Verify Redis Connection

```bash
redis-cli ping
# Should return: PONG
```

### 3. Start Application

The application will automatically connect to Redis and enable caching.

### 4. Monitor Cache

Use Redis CLI to monitor cache:
```bash
redis-cli
> KEYS shuttle-system:*
> TTL shuttle-system:routes::all
```

## Best Practices

### 1. Cache Key Design
- Use descriptive, unique keys
- Include parameters that affect the result
- Keep keys reasonably short

### 2. TTL Strategy
- **Static Data** (stops): Longer TTL (15 minutes)
- **Semi-static Data** (routes): Medium TTL (10 minutes)
- **Dynamic Data** (bookings): Shorter TTL (5 minutes)
- **Analytics**: Medium TTL (5 minutes)

### 3. Cache Invalidation
- Invalidate related caches when data changes
- Use `@CacheEvict` annotations for automatic invalidation
- Manual invalidation for complex scenarios

### 4. Monitoring
- Monitor cache hit rates
- Set up alerts for cache failures
- Regular cache cleanup for unused data

## Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Check if Redis is running
   - Verify connection settings in `application.properties`
   - Check firewall settings

2. **Cache Not Working**
   - Verify `@EnableCaching` annotation is present
   - Check cache annotations are properly applied
   - Monitor cache keys in Redis

3. **Memory Issues**
   - Monitor Redis memory usage
   - Adjust TTL settings
   - Implement cache size limits

### Debug Commands

```bash
# Check Redis status
redis-cli ping

# List all cache keys
redis-cli KEYS shuttle-system:*

# Check specific cache TTL
redis-cli TTL shuttle-system:routes::all

# Monitor Redis operations
redis-cli MONITOR
```

## Future Enhancements

1. **Cache Warming**: Pre-populate frequently accessed data
2. **Distributed Caching**: Redis cluster for high availability
3. **Cache Metrics**: Integration with monitoring tools
4. **Smart Invalidation**: Event-driven cache invalidation
5. **Cache Compression**: Reduce memory usage for large objects 