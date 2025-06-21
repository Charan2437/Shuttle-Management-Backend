package com.shuttle.shuttlesystem.service;

import java.util.List;
import java.util.UUID;

import com.shuttle.shuttlesystem.dto.ShuttleDTO;

public interface ShuttleService {
    List<ShuttleDTO> getAllShuttles();
    ShuttleDTO getShuttleById(UUID id);
    ShuttleDTO createShuttle(ShuttleDTO dto);
    ShuttleDTO updateShuttle(UUID id, ShuttleDTO dto);
    void deleteShuttle(UUID id);
    List<ShuttleDTO> getAvailableShuttles(); // Shuttles not assigned to any route
    List<ShuttleDTO> getAssignedShuttles(); // Shuttles assigned to routes
} 