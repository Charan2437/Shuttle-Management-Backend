package com.shuttle.shuttlesystem.service;

import java.util.Map;

public interface AdminAnalyticsService {
    Map<String, Object> getOverview(String fromDate, String toDate);
    Map<String, Object> getRouteAnalytics(String fromDate, String toDate);
    Map<String, Object> getStudentAnalytics(String fromDate, String toDate);
    Map<String, Object> getRouteHourlyBookings(String fromDate, String toDate);
    Map<String, Object> getRouteHourlyBookingsByName(String routeName, String fromDate, String toDate);
}
