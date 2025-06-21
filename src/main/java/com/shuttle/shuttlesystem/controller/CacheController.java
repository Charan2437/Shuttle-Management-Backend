package com.shuttle.shuttlesystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.service.CacheService;

@RestController
@RequestMapping("/api/admin/cache")
@PreAuthorize("hasRole('ADMIN')")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Check if Redis is available
        boolean redisAvailable = cacheService.hasKey("health-check");
        if (redisAvailable) {
            cacheService.delete("health-check");
        } else {
            cacheService.setWithTTL("health-check", "ok", 60);
        }
        
        status.put("redisAvailable", redisAvailable);
        status.put("message", "Cache status retrieved successfully");
        
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/routes")
    public ResponseEntity<Map<String, Object>> clearRoutesCache() {
        cacheService.invalidateRouteCaches();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Routes cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/stops")
    public ResponseEntity<Map<String, Object>> clearStopsCache() {
        cacheService.clearCache("stops");
        cacheService.invalidateRouteCaches();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Stops cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/shuttles")
    public ResponseEntity<Map<String, Object>> clearShuttlesCache() {
        cacheService.invalidateShuttleCaches();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shuttles cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/bookings")
    public ResponseEntity<Map<String, Object>> clearBookingsCache() {
        cacheService.invalidateBookingCaches();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Bookings cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/analytics")
    public ResponseEntity<Map<String, Object>> clearAnalyticsCache() {
        cacheService.clearCache("analytics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Analytics cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/student-stats")
    public ResponseEntity<Map<String, Object>> clearStudentStatsCache() {
        cacheService.clearCache("student-stats");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Student stats cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/wallet")
    public ResponseEntity<Map<String, Object>> clearWalletCache() {
        cacheService.invalidateWalletCaches();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Wallet cache cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        cacheService.invalidateRouteCaches();
        cacheService.invalidateShuttleCaches();
        cacheService.invalidateBookingCaches();
        cacheService.invalidateWalletCaches();
        cacheService.clearCache("stops");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All caches cleared successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, Object>> clearSpecificCache(@PathVariable String cacheName) {
        cacheService.clearCache(cacheName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cache '" + cacheName + "' cleared successfully");
        
        return ResponseEntity.ok(response);
    }
} 