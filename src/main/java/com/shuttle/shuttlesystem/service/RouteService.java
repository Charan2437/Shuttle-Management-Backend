package com.shuttle.shuttlesystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.shuttle.shuttlesystem.dto.RoutePlanOptionDTO;
import com.shuttle.shuttlesystem.dto.RouteWithStopsAndHoursDTO;

public interface RouteService {
    List<RouteWithStopsAndHoursDTO> getAllRoutes();
    RouteWithStopsAndHoursDTO getRouteById(UUID id);
    RouteWithStopsAndHoursDTO createRoute(RouteWithStopsAndHoursDTO dto);
    RouteWithStopsAndHoursDTO updateRoute(UUID id, RouteWithStopsAndHoursDTO dto);
    void deleteRoute(UUID id);
    List<RoutePlanOptionDTO> planRoutes(UUID startStopId, UUID endStopId, LocalDateTime departureTime, int maxTransfers);
    void assignShuttleToRoute(String shuttleName, UUID routeId);
}
