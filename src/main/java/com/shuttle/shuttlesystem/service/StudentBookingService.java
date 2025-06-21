package com.shuttle.shuttlesystem.service;

import java.util.List;
import java.util.Map;

import com.shuttle.shuttlesystem.dto.BookingConfirmRequestDTO;

public interface StudentBookingService {
    Map<String, Object> confirmBooking(BookingConfirmRequestDTO request, String email);
    List<Map<String, Object>> getTripHistory(String email, int limit, int offset, String fromDate, String toDate, String status);
    List<Map<String, Object>> getFrequentRoutes(String email, int limit, String fromDate, String toDate);
    Map<String, Object> getTravelAnalytics(String email, String fromDate, String toDate);
    Map<String, Object> getUpcomingBooking(String email);
    Map<String, Object> markBookingCompleted(String email, String bookingId);
    Map<String, Object> cancelBooking(String email, String bookingId);
}
