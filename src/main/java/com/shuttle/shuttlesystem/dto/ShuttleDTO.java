package com.shuttle.shuttlesystem.dto;

import java.util.UUID;

public class ShuttleDTO {
    public UUID id;
    public String shuttleNo;
    public int capacity;
    public String routeName; // Associated route name if any
    public boolean isAssigned; // Whether the shuttle is assigned to a route
} 