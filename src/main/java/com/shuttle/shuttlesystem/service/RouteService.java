package com.shuttle.shuttlesystem.service;

import com.shuttle.shuttlesystem.dto.RouteWithStopsAndHoursDTO;
import com.shuttle.shuttlesystem.model.Route;
import java.util.List;
import java.util.UUID;

public interface RouteService {
    List<RouteWithStopsAndHoursDTO> getAllRoutes();
    RouteWithStopsAndHoursDTO getRouteById(UUID id);
    RouteWithStopsAndHoursDTO createRoute(RouteWithStopsAndHoursDTO dto);
    RouteWithStopsAndHoursDTO updateRoute(UUID id, RouteWithStopsAndHoursDTO dto);
    void deleteRoute(UUID id);
}
