package com.shuttle.shuttlesystem.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.BookingConfirmRequestDTO;
import com.shuttle.shuttlesystem.service.CacheService;
import com.shuttle.shuttlesystem.service.StudentBookingService;

@Service
public class StudentBookingServiceImpl implements StudentBookingService {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Transactional
    @CacheEvict(value = {"bookings", "student-stats", "analytics"}, allEntries = true)
    public Map<String, Object> confirmBooking(BookingConfirmRequestDTO request, String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Lookup studentId from email
            UUID studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                new Object[]{email}, UUID.class);
            if (studentId == null) {
                result.put("success", false);
                result.put("message", "Student not found");
                return result;
            }
            // 2. Validate input
            if (request.legs == null || request.legs.isEmpty() || request.totalCost <= 0) {
                result.put("success", false);
                result.put("message", "Invalid input");
                return result;
            }
            // 3. Lock student row and check wallet balance
            Integer walletBalance = jdbcTemplate.queryForObject(
                "SELECT wallet_balance FROM students WHERE id = ? FOR UPDATE",
                new Object[]{studentId}, Integer.class);
            if (walletBalance == null) {
                result.put("success", false);
                result.put("message", "Student not found");
                return result;
            }
            if (walletBalance < request.totalCost) {
                result.put("success", false);
                result.put("message", "Insufficient balance");
                return result;
            }
            // 4. Check shuttle capacity for each leg
            // Skipped: Assume infinite seats for now
            // 5. Deduct points from wallet
            jdbcTemplate.update(
                "UPDATE students SET wallet_balance = wallet_balance - ? WHERE id = ?",
                request.totalCost, studentId);
            // 6. Create main booking record
            UUID bookingId = UUID.randomUUID();
            String bookingReference = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            jdbcTemplate.update(
                "INSERT INTO bookings (id, student_id, route_id, from_stop_id, to_stop_id, scheduled_time, points_deducted, booking_reference, status_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, (SELECT id FROM booking_status_types WHERE name = 'confirmed'))",
                bookingId, studentId, request.legs.get(0).routeId, request.legs.get(0).fromStopId, request.legs.get(0).toStopId, request.legs.get(0).scheduledTime, request.totalCost, bookingReference
            );
            // 7. Create transfer bookings if multiple legs
            if (request.legs.size() > 1) {
                for (int i = 1; i < request.legs.size(); i++) {
                    BookingConfirmRequestDTO.Leg leg = request.legs.get(i);
                    BookingConfirmRequestDTO.Leg prevLeg = request.legs.get(i - 1);
                    UUID transferId = UUID.randomUUID();
                    jdbcTemplate.update(
                        "INSERT INTO transfer_bookings (id, main_booking_id, route_id, from_stop_id, to_stop_id, transfer_stop_id, scheduled_time, estimated_wait_time, transfer_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        transferId, bookingId, leg.routeId, leg.fromStopId, leg.toStopId, prevLeg.toStopId, leg.scheduledTime, 0, i
                    );
                }
            }
            // 8. Create wallet transaction record
            UUID transactionId = UUID.randomUUID();
            jdbcTemplate.update(
                "INSERT INTO wallet_transactions (id, student_id, amount, transaction_type_id, booking_id, description) VALUES (?, ?, ?, (SELECT id FROM transaction_types WHERE name = 'booking'), ?, ?)",
                transactionId, studentId, -request.totalCost, bookingId, "Booking: " + bookingReference
            );
            // 9. Return success response
            result.put("success", true);
            result.put("bookingId", bookingId.toString());
            result.put("bookingReference", bookingReference);
            result.put("message", "Booking confirmed successfully");
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Booking failed: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Cacheable(value = "bookings", key = "'trip-history-' + #email + '-' + #limit + '-' + #offset + '-' + #fromDate + '-' + #toDate + '-' + #status")
    public List<Map<String, Object>> getTripHistory(String email, int limit, int offset, String fromDate, String toDate, String status) {
        // 1. Lookup studentId from email
        UUID studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",

            new Object[]{email}, UUID.class);
        if (studentId == null) return Collections.emptyList();

        // 2. Build SQL query
        StringBuilder query = new StringBuilder(
            "SELECT b.id AS bookingId, r.name AS routeName, fs.name AS fromStop, ts.name AS toStop, " +
            "b.scheduled_time AS scheduledTime, bst.name AS status, b.points_deducted AS pointsDeducted, " +
            "b.booking_reference AS bookingReference, b.created_at AS createdAt " +
            "FROM bookings b " +
            "JOIN routes r ON b.route_id = r.id " +
            "JOIN stops fs ON b.from_stop_id = fs.id " +
            "JOIN stops ts ON b.to_stop_id = ts.id " +
            "JOIN booking_status_types bst ON b.status_id = bst.id " +
            "WHERE b.student_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        if (fromDate != null && !fromDate.isEmpty()) {
            query.append(" AND b.scheduled_time >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            query.append(" AND b.scheduled_time <= ?");
            params.add(toDate);
        }
        if (status != null && !status.isEmpty()) {
            query.append(" AND bst.name = ?");
            params.add(status);
        }
        query.append(" ORDER BY b.scheduled_time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(query.toString(), params.toArray());
        for (Map<String, Object> booking : bookings) {
            UUID bookingId = (UUID) booking.get("bookingId");
            List<Map<String, Object>> transfers = jdbcTemplate.queryForList(
                "SELECT sfrom.name AS fromStop, sto.name AS toStop, s.name AS transferStop, tb.estimated_wait_time, tb.transfer_order " +
                "FROM transfer_bookings tb " +
                "JOIN stops sfrom ON tb.from_stop_id = sfrom.id " +
                "JOIN stops sto ON tb.to_stop_id = sto.id " +
                "JOIN stops s ON tb.transfer_stop_id = s.id " +
                "WHERE tb.main_booking_id = ? " +
                "ORDER BY tb.transfer_order ASC",
                bookingId
            );
            booking.put("transfers", transfers);
        }
        return bookings;
    }

    @Override
    @Cacheable(value = "bookings", key = "'frequent-routes-' + #email + '-' + #limit + '-' + #fromDate + '-' + #toDate")
    public List<Map<String, Object>> getFrequentRoutes(String email, int limit, String fromDate, String toDate) {
        UUID studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",
            new Object[]{email}, UUID.class);
        if (studentId == null) return Collections.emptyList();

        StringBuilder query = new StringBuilder(
            "SELECT b.route_id AS routeId, r.name AS routeName, fs.name AS fromStop, ts.name AS toStop, " +
            "COUNT(*) AS tripCount, MAX(b.scheduled_time) AS lastUsed " +
            "FROM bookings b " +
            "JOIN routes r ON b.route_id = r.id " +
            "JOIN stops fs ON b.from_stop_id = fs.id " +
            "JOIN stops ts ON b.to_stop_id = ts.id " +
            "WHERE b.student_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        if (fromDate != null && !fromDate.isEmpty()) {
            query.append(" AND b.scheduled_time >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            query.append(" AND b.scheduled_time <= ?");
            params.add(toDate);
        }
        query.append(" GROUP BY b.route_id, r.name, fs.name, ts.name ORDER BY tripCount DESC, lastUsed DESC LIMIT ?");
        params.add(limit);
        return jdbcTemplate.queryForList(query.toString(), params.toArray());
    }

    @Override
    @Cacheable(value = "bookings", key = "'travel-analytics-' + #email + '-' + #fromDate + '-' + #toDate")
    public Map<String, Object> getTravelAnalytics(String email, String fromDate, String toDate) {
        UUID studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",
            new Object[]{email}, UUID.class);
        if (studentId == null) return Collections.emptyMap();

        StringBuilder baseQuery = new StringBuilder(
            "SELECT b.*, r.name AS routeName, fs.name AS fromStop, ts.name AS toStop " +
            "FROM bookings b " +
            "JOIN routes r ON b.route_id = r.id " +
            "JOIN stops fs ON b.from_stop_id = fs.id " +
            "JOIN stops ts ON b.to_stop_id = ts.id " +
            "WHERE b.student_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        if (fromDate != null && !fromDate.isEmpty()) {
            baseQuery.append(" AND b.scheduled_time >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isEmpty()) {
            baseQuery.append(" AND b.scheduled_time <= ?");
            params.add(toDate);
        }
        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(baseQuery.toString(), params.toArray());
        int totalTrips = bookings.size();
        int totalPointsSpent = bookings.stream().mapToInt(b -> b.get("points_deducted") != null ? ((Number)b.get("points_deducted")).intValue() : 0).sum();
        double avgRating = bookings.stream().filter(b -> b.get("rating") != null)
            .mapToDouble(b -> ((Number)b.get("rating")).doubleValue()).average().orElse(0.0);
        // Favorite route
        Map<String, Integer> routeCounts = new HashMap<>();
        Map<String, Map<String, Object>> routeSample = new HashMap<>();
        for (Map<String, Object> b : bookings) {
            String key = b.get("route_id") + "|" + b.get("from_stop_id") + "|" + b.get("to_stop_id");
            routeCounts.put(key, routeCounts.getOrDefault(key, 0) + 1);
            if (!routeSample.containsKey(key)) routeSample.put(key, b);
        }
        String favoriteRouteKey = routeCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);
        Map<String, Object> favoriteRoute = null;
        if (favoriteRouteKey != null) {
            Map<String, Object> sample = routeSample.get(favoriteRouteKey);
            favoriteRoute = new HashMap<>();
            favoriteRoute.put("routeId", sample.get("route_id"));
            favoriteRoute.put("routeName", sample.get("routeName"));
            favoriteRoute.put("fromStop", sample.get("fromStop"));
            favoriteRoute.put("toStop", sample.get("toStop"));
            favoriteRoute.put("tripCount", routeCounts.get(favoriteRouteKey));
        }
        // Usage pattern
        String[] periods = {"Morning (6AM-12PM)", "Afternoon (12PM-6PM)", "Evening (6PM-12AM)"};
        int[] periodStarts = {6, 12, 18};
        int[] periodEnds = {12, 18, 24};
        List<Map<String, Object>> usagePattern = new ArrayList<>();
        int[] tripsPerPeriod = new int[3];
        for (Map<String, Object> b : bookings) {
            Object schedObj = b.get("scheduled_time");
            java.sql.Timestamp sched = schedObj instanceof java.sql.Timestamp ? (java.sql.Timestamp)schedObj : java.sql.Timestamp.valueOf(b.get("scheduled_time").toString().replace("T", " "));
            int hour = sched.toLocalDateTime().getHour();
            for (int i = 0; i < 3; i++) {
                if (hour >= periodStarts[i] && hour < periodEnds[i]) {
                    tripsPerPeriod[i]++;
                }
            }
        }
        int maxTrips = 0, maxIdx = -1;
        for (int i = 0; i < 3; i++) {
            int trips = tripsPerPeriod[i];
            if (trips > maxTrips) { maxTrips = trips; maxIdx = i; }
            Map<String, Object> period = new HashMap<>();
            period.put("period", periods[i]);
            period.put("percentage", totalTrips == 0 ? 0 : Math.round((trips * 100.0) / totalTrips));
            period.put("trips", trips);
            usagePattern.add(period);
        }
        String peakTime = maxIdx >= 0 ? periods[maxIdx] : null;
        // Points saved (stub, implement logic as needed)
        int pointsSaved = 0;
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalTrips", totalTrips);
        analytics.put("totalPointsSpent", totalPointsSpent);
        analytics.put("avgRating", Math.round(avgRating * 10.0) / 10.0);
        analytics.put("favoriteRoute", favoriteRoute);
        analytics.put("peakTime", peakTime);
        analytics.put("usagePattern", usagePattern);
        analytics.put("pointsSaved", pointsSaved);
        return analytics;
    }

    @Override
    public Map<String, Object> getUpcomingBooking(String email) {
        UUID studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",
            new Object[]{email}, UUID.class);
        if (studentId == null) return Collections.emptyMap();
        String sql = "SELECT b.id AS bookingId, r.name AS routeName, fs.name AS fromStop, ts.name AS toStop, " +
                "b.scheduled_time AS scheduledTime, bst.name AS status, b.points_deducted AS pointsDeducted, " +
                "b.booking_reference AS bookingReference, b.created_at AS createdAt " +
                "FROM bookings b " +
                "JOIN routes r ON b.route_id = r.id " +
                "JOIN stops fs ON b.from_stop_id = fs.id " +
                "JOIN stops ts ON b.to_stop_id = ts.id " +
                "JOIN booking_status_types bst ON b.status_id = bst.id " +
                "WHERE b.student_id = ? AND b.scheduled_time > now() AND bst.name = 'confirmed' " +
                "ORDER BY b.scheduled_time ASC LIMIT 1";
        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(sql, studentId);
        return bookings.isEmpty() ? Collections.emptyMap() : bookings.get(0);
    }

    @Override
    public Map<String, Object> markBookingCompleted(String email, String bookingId) {
        UUID studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",
            new Object[]{email}, UUID.class);
        if (studentId == null) return Collections.singletonMap("success", false);
        UUID statusId = jdbcTemplate.queryForObject(
            "SELECT id FROM booking_status_types WHERE name = 'completed'", new Object[]{}, UUID.class);
        int updated = jdbcTemplate.update(
            "UPDATE bookings SET status_id = ?, updated_at = now() WHERE id = ? AND student_id = ?",
            statusId, UUID.fromString(bookingId), studentId);
        return Collections.singletonMap("success", updated > 0);
    }

    @Override
    public Map<String, Object> cancelBooking(String email, String bookingId) {
        UUID studentId = jdbcTemplate.queryForObject(
            "SELECT id FROM students WHERE user_id = (SELECT id FROM users WHERE email = ?)",
            new Object[]{email}, UUID.class);
        if (studentId == null) return Collections.singletonMap("success", false);
        UUID statusId = jdbcTemplate.queryForObject(
            "SELECT id FROM booking_status_types WHERE name = 'cancelled'", new Object[]{}, UUID.class);
        // Count previous cancellations
        Integer cancelCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bookings WHERE student_id = ? AND status_id = ?",
            new Object[]{studentId, statusId}, Integer.class);
        // Get trip cost
        Integer pointsDeducted = jdbcTemplate.queryForObject(
            "SELECT points_deducted FROM bookings WHERE id = ? AND student_id = ?",
            new Object[]{UUID.fromString(bookingId), studentId}, Integer.class);
        // Cancel booking
        int updated = jdbcTemplate.update(
            "UPDATE bookings SET status_id = ?, cancelled_at = now(), updated_at = now() WHERE id = ? AND student_id = ?",
            statusId, UUID.fromString(bookingId), studentId);
        // Deduct penalty if more than 3 cancellations
        if (cancelCount != null && cancelCount >= 3 && pointsDeducted != null && updated > 0) {
            int penalty = (int)Math.ceil(pointsDeducted * 0.1);
            jdbcTemplate.update(
                "UPDATE students SET wallet_balance = wallet_balance - ? WHERE id = ?",
                penalty, studentId);
            // Record penalty transaction
            UUID txId = UUID.randomUUID();
            UUID txTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM transaction_types WHERE name = 'debit'", new Object[]{}, UUID.class);
            jdbcTemplate.update(
                "INSERT INTO wallet_transactions (id, student_id, transaction_type_id, amount, booking_id, description, created_at) VALUES (?, ?, ?, ?, ?, ?, now())",
                txId, studentId, txTypeId, -penalty, UUID.fromString(bookingId), "Penalty for excessive cancellations");
        }
        return Collections.singletonMap("success", updated > 0);
    }
}
