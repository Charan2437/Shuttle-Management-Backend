package com.shuttle.shuttlesystem.dto;

import java.util.Date;
import java.util.UUID;
import java.util.List;

public class BookingAdminDTO {
    public UUID id;
    public UUID studentId;
    public UUID routeId;
    public UUID fromStopId;
    public UUID toStopId;
    public Date scheduledTime;
    public String status;
    public int pointsDeducted;
    public String bookingReference;
    public String notes;
    public Date createdAt;
    public StudentInfo student;
    public RouteInfo route;
    public StopInfo fromStop;
    public StopInfo toStop;
    public List<TransferBookingDTO> transferBookings;

    public static class StudentInfo {
        public String name;
        public String studentId;
    }
    public static class RouteInfo {
        public String name;
        public String color;
    }
    public static class StopInfo {
        public String name;
    }
    public static class TransferBookingDTO {
        public UUID id;
        public UUID fromStopId;
        public UUID toStopId;
        public UUID transferStopId;
        public int estimatedWaitTime;
        public int transferOrder;
    }
}
