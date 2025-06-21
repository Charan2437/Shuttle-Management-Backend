package com.shuttle.shuttlesystem.dto;

import java.util.*;

public class RouteWithStopsAndHoursDTO {
    public UUID id;
    public String name;
    public String description;
    public String color;
    public int estimatedDuration;
    public int baseFare;
    public boolean isActive;
    public List<StopDTO> stops;
    public List<RouteOperatingHourDTO> operatingHours;
    public String shuttleName; // Add this field for shuttle name

    public static class StopDTO {
        public UUID id;
        public String name;
        public String description;
        public double latitude;
        public double longitude;
        public String address;
        public int stopOrder;
        public Integer estimatedTravelTime;
        public Double distanceFromPrevious;
    }

    public static class RouteOperatingHourDTO {
        public UUID id;
        public int dayOfWeek;
        public String startTime;
        public String endTime;
    }
}
