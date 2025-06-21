package com.shuttle.shuttlesystem.dto;

import java.util.List;
import java.util.UUID;

public class RoutePlanOptionDTO {
    public List<UUID> stops;
    public List<LegDetail> legs;
    public double total_distance;
    public int total_time;
    public double total_cost;
    public double max_crowding;

    public static class LegDetail {
        public UUID route_id;
        public String route_name;
        public UUID shuttle_id;
        public String shuttle_no;
        public UUID from;
        public UUID to;
        public int time;
        public double distance;
        public double cost;
        public double crowd;
    }
}
