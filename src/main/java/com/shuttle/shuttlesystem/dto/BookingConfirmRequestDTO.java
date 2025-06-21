package com.shuttle.shuttlesystem.dto;

import java.util.List;

public class BookingConfirmRequestDTO {
    public List<Leg> legs;
    public double totalCost;

    public static class Leg {
        public String routeId;
        public String fromStopId;
        public String toStopId;
        public String scheduledTime;
        public double cost;
    }
}
