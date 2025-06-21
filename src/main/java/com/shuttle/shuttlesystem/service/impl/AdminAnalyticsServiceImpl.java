package com.shuttle.shuttlesystem.service.impl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.shuttle.shuttlesystem.service.AdminAnalyticsService;
import com.shuttle.shuttlesystem.service.CacheService;

@Service
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Cacheable(value = "analytics", key = "'overview-' + #fromDate + '-' + #toDate")
    public Map<String, Object> getOverview(String fromDate, String toDate) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE (bst.name = 'completed' OR bst.name = 'boarded') ");
        if (fromDate != null && !fromDate.isEmpty()) {
            where.append(" AND b.created_at >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            where.append(" AND b.created_at <= ?");
            params.add(toDate);
        }
        // 1. Total active students
        Integer totalActiveStudents = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT b.student_id) FROM bookings b JOIN booking_status_types bst ON b.status_id = bst.id" + where,
            params.toArray(), Integer.class);
        // 2. Average bookings per day
        Map<String, Object> bookingsResult = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS total, MIN(DATE(b.created_at)) AS min_date, MAX(DATE(b.created_at)) AS max_date " +
            "FROM bookings b JOIN booking_status_types bst ON b.status_id = bst.id" + where,
            params.toArray());
        int totalBookings = ((Number)bookingsResult.get("total")).intValue();
        Date minDate = (Date) bookingsResult.get("min_date");
        Date maxDate = (Date) bookingsResult.get("max_date");
        double averageBookingsPerDay = totalBookings;
        if (minDate != null && maxDate != null) {
            long days = (maxDate.toLocalDate().toEpochDay() - minDate.toLocalDate().toEpochDay()) + 1;
            if (days > 0) averageBookingsPerDay = totalBookings / (double) days;
        }
        // 3. Hourly booking pattern
        List<Map<String, Object>> hourlyPattern = jdbcTemplate.queryForList(
            "SELECT EXTRACT(HOUR FROM b.scheduled_time) AS hour, COUNT(*) AS count " +
            "FROM bookings b JOIN booking_status_types bst ON b.status_id = bst.id" + where +
            " GROUP BY hour ORDER BY hour",
            params.toArray());
        // 4. Peak hour
        Map<String, Object> peakHour = null;
        for (Map<String, Object> row : hourlyPattern) {
            if (peakHour == null || ((Number)row.get("count")).intValue() > ((Number)peakHour.get("count")).intValue()) {
                peakHour = row;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalActiveStudents", totalActiveStudents);
        result.put("averageBookingsPerDay", Math.round(averageBookingsPerDay * 100.0) / 100.0);
        result.put("hourlyBookingPattern", hourlyPattern);
        if (peakHour != null) {
            Map<String, Object> peak = new HashMap<>();
            peak.put("hour", ((Number)peakHour.get("hour")).intValue());
            peak.put("count", ((Number)peakHour.get("count")).intValue());
            result.put("peakHour", peak);
        } else {
            result.put("peakHour", null);
        }
        return result;
    }

    @Override
    @Cacheable(value = "analytics", key = "'route-analytics-' + #fromDate + '-' + #toDate")
    public Map<String, Object> getRouteAnalytics(String fromDate, String toDate) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (fromDate != null && !fromDate.isEmpty()) {
            where.append(" AND b.created_at >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            where.append(" AND b.created_at <= ?");
            params.add(toDate);
        }
        String sql = "SELECT r.id AS routeId, r.name AS routeName, " +
                "COUNT(b.id) AS bookings, " +
                "COALESCE(SUM(b.points_deducted), 0) AS revenue, " +
                "ROUND(100.0 * SUM(CASE WHEN bst.name = 'completed' THEN 1 ELSE 0 END) / NULLIF(COUNT(b.id), 0), 2) AS efficiency, " +
                "ROUND(100.0 * SUM(CASE WHEN bst.name = 'completed' THEN 1 ELSE 0 END) / NULLIF(COUNT(b.id), 0), 2) AS onTime, " +
                "ROUND(100.0 * SUM(CASE WHEN bst.name = 'cancelled' THEN 1 ELSE 0 END) / NULLIF(COUNT(b.id), 0), 2) AS cancelled " +
                "FROM routes r " +
                "LEFT JOIN bookings b ON b.route_id = r.id " +
                "LEFT JOIN booking_status_types bst ON b.status_id = bst.id " +
                where.toString() +
                " GROUP BY r.id, r.name ORDER BY bookings DESC";
        List<Map<String, Object>> routes = jdbcTemplate.queryForList(sql, params.toArray());
        // Cleaned routes response
        List<Map<String, Object>> cleanedRoutes = new ArrayList<>();
        for (Map<String, Object> r : routes) {
            Map<String, Object> cleaned = new HashMap<>();
            cleaned.put("routeId", r.get("routeId"));
            cleaned.put("routeName", r.get("routeName"));
            cleaned.put("bookings", r.get("bookings"));
            cleaned.put("efficiency", r.get("efficiency"));
            cleaned.put("revenue", r.get("revenue"));
            cleaned.put("onTime", r.get("onTime"));
            // Remove delayed, and set cancelled to 0 if null
            Object cancelled = r.get("cancelled");
            cleaned.put("cancelled", cancelled == null ? 0 : cancelled);
            cleanedRoutes.add(cleaned);
        }
        // Top routes by bookings
        List<Map<String, Object>> topRoutes = new ArrayList<>();
        for (int i = 0; i < Math.min(5, cleanedRoutes.size()); i++) {
            Map<String, Object> r = cleanedRoutes.get(i);
            Map<String, Object> top = new HashMap<>();
            top.put("routeId", r.get("routeId"));
            top.put("routeName", r.get("routeName"));
            top.put("bookings", r.get("bookings"));
            topRoutes.add(top);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("routes", cleanedRoutes);
        result.put("topRoutes", topRoutes);
        return result;
    }

    @Override
    @Cacheable(value = "analytics", key = "'student-analytics-' + #fromDate + '-' + #toDate")
    public Map<String, Object> getStudentAnalytics(String fromDate, String toDate) {
        List<Object> params = new ArrayList<>();
        StringBuilder bookingWhere = new StringBuilder(" WHERE 1=1 ");
        if (fromDate != null && !fromDate.isEmpty()) {
            bookingWhere.append(" AND b.created_at >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            bookingWhere.append(" AND b.created_at <= ?");
            params.add(toDate);
        }
        // Total students
        Integer totalStudents = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM students",
            Integer.class);
        // Active students
        Integer activeStudents = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT b.student_id) FROM bookings b JOIN booking_status_types bst ON b.status_id = bst.id " +
            bookingWhere + " AND (bst.name = 'completed' OR bst.name = 'boarded')",
            params.toArray(), Integer.class);
        // New registrations (this would need a different approach - using student creation date)
        Integer newRegistrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM students",
            Integer.class);
        // Total trips
        Integer totalTrips = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bookings b JOIN booking_status_types bst ON b.status_id = bst.id " +
            bookingWhere + " AND (bst.name = 'completed' OR bst.name = 'boarded')",
            params.toArray(), Integer.class);
        double avgTripsPerStudent = (activeStudents != null && activeStudents > 0) ? (double) totalTrips / activeStudents : 0.0;
        // User segments
        List<Map<String, Object>> segments = new ArrayList<>();
        // Frequent: 10+ trips/week, Regular: 5-9, Occasional: 1-4, Inactive: 0
        StringBuilder segWhere = new StringBuilder();
        List<Object> segParams = new ArrayList<>();
        if (fromDate != null && !fromDate.isEmpty()) {
            segWhere.append(" AND b.created_at >= ?");
            segParams.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            segWhere.append(" AND b.created_at <= ?");
            segParams.add(toDate);
        }
        
        String segSql = "SELECT s.id, u.name, COUNT(b.id) AS tripCount FROM students s " +
            "JOIN users u ON s.user_id = u.id " +
            "LEFT JOIN bookings b ON b.student_id = s.id AND b.status_id IN (SELECT id FROM booking_status_types WHERE name IN ('completed', 'boarded')) " +
            (segWhere.length() > 0 ? segWhere.toString() : "") +
            " GROUP BY s.id, u.name";
        List<Map<String, Object>> segData = jdbcTemplate.queryForList(segSql, segParams.toArray());
        int frequent = 0, regular = 0, occasional = 0, inactive = 0;
        for (Map<String, Object> row : segData) {
            int trips = ((Number)row.get("tripCount")).intValue();
            if (trips >= 10) frequent++;
            else if (trips >= 5) regular++;
            else if (trips >= 1) occasional++;
            else inactive++;
        }
        segments.add(Map.of("label", "Frequent Users", "count", frequent, "criteria", "10+ trips/week"));
        segments.add(Map.of("label", "Regular Users", "count", regular, "criteria", "5-9 trips/week"));
        segments.add(Map.of("label", "Occasional Users", "count", occasional, "criteria", "1-4 trips/week"));
        segments.add(Map.of("label", "Inactive Users", "count", inactive, "criteria", "0 trips/week"));
        // Popular routes by student type (example: Frequent Users)
        List<Map<String, Object>> popularRoutes = new ArrayList<>();
        String popSql = "SELECT r.name AS routeName, COUNT(b.id) AS cnt FROM bookings b " +
            "JOIN routes r ON b.route_id = r.id " +
            "JOIN booking_status_types bst ON b.status_id = bst.id " +
            bookingWhere + " AND (bst.name = 'completed' OR bst.name = 'boarded') " +
            "GROUP BY r.name ORDER BY cnt DESC LIMIT 1";
        if (frequent > 0) {
            Map<String, Object> pr = jdbcTemplate.queryForMap(popSql, params.toArray());
            popularRoutes.add(Map.of("segment", "Frequent Users", "routeName", pr.get("routeName"), "percentage", 100));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("totalStudents", totalStudents);
        result.put("activeStudents", activeStudents);
        result.put("newRegistrations", newRegistrations);
        result.put("avgTripsPerStudent", Math.round(avgTripsPerStudent * 100.0) / 100.0);
        result.put("userSegments", segments);
        result.put("popularRoutesByStudentType", popularRoutes);
        return result;
    }

    @Override
    public Map<String, Object> getRouteHourlyBookingsByName(String routeName, String fromDate, String toDate) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE r.name = ? ");
        params.add(routeName);
        if (fromDate != null && !fromDate.isEmpty()) {
            where.append(" AND b.created_at >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            where.append(" AND b.created_at <= ?");
            params.add(toDate);
        }
        String sql = "SELECT EXTRACT(HOUR FROM b.scheduled_time) AS hour, COUNT(*) AS count " +
                "FROM bookings b " +
                "JOIN routes r ON b.route_id = r.id " +
                "JOIN booking_status_types bst ON b.status_id = bst.id " +
                where.toString() +
                " AND (bst.name = 'completed' OR bst.name = 'boarded') " +
                "GROUP BY hour ORDER BY hour";
        List<Map<String, Object>> hourlyPattern = jdbcTemplate.queryForList(sql, params.toArray());
        // Fill in hours 6-23 with 0 if missing
        Map<Integer, Integer> hourMap = new java.util.LinkedHashMap<>();
        for (int h = 6; h <= 23; h++) hourMap.put(h, 0);
        for (Map<String, Object> row : hourlyPattern) {
            int hour = ((Number)row.get("hour")).intValue();
            int count = ((Number)row.get("count")).intValue();
            if (hourMap.containsKey(hour)) hourMap.put(hour, count);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("routeName", routeName);
        result.put("hourlyBookingPattern", hourMap);
        return result;
    }

    @Override
    public Map<String, Object> getRouteHourlyBookings(String fromDate, String toDate) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        if (fromDate != null && !fromDate.isEmpty()) {
            where.append(" AND b.created_at >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            where.append(" AND b.created_at <= ?");
            params.add(toDate);
        }
        String sql = "SELECT EXTRACT(HOUR FROM b.scheduled_time) AS hour, COUNT(*) AS count " +
                "FROM bookings b " +
                "JOIN booking_status_types bst ON b.status_id = bst.id " +
                where.toString() +
                " AND (bst.name = 'completed' OR bst.name = 'boarded') " +
                "GROUP BY hour ORDER BY hour";
        List<Map<String, Object>> hourlyPattern = jdbcTemplate.queryForList(sql, params.toArray());
        // Fill in hours 6-23 with 0 if missing
        Map<Integer, Integer> hourMap = new java.util.LinkedHashMap<>();
        for (int h = 6; h <= 23; h++) hourMap.put(h, 0);
        for (Map<String, Object> row : hourlyPattern) {
            int hour = ((Number)row.get("hour")).intValue();
            int count = ((Number)row.get("count")).intValue();
            if (hourMap.containsKey(hour)) hourMap.put(hour, count);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("hourlyBookingPattern", hourMap);
        return result;
    }
}
