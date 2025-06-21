package com.shuttle.shuttlesystem.dto;

import java.util.List;
import java.util.Map;

public class StudentWalletAnalyticsDTO {
    public List<Map<String, Object>> monthlyCredits;
    public List<Map<String, Object>> monthlyDebits;
    public int totalTrips;
    public int avgCostPerTrip;
    public String mostUsedRoute;
    public String peakUsageTime;
    public int pointsSaved;
}
