package com.shuttle.shuttlesystem.controller;

import com.shuttle.shuttlesystem.dto.RouteWithStopsAndHoursDTO;
import com.shuttle.shuttlesystem.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/routes")
public class RouteController {
    @Autowired
    private RouteService routeService;

    @GetMapping
    public List<RouteWithStopsAndHoursDTO> getAllRoutes() {
        return routeService.getAllRoutes();
    }

    @GetMapping("/{id}")
    public RouteWithStopsAndHoursDTO getRouteById(@PathVariable UUID id) {
        return routeService.getRouteById(id);
    }

    @PostMapping
    public RouteWithStopsAndHoursDTO createRoute(@RequestBody RouteWithStopsAndHoursDTO dto) {
        return routeService.createRoute(dto);
    }

    @PutMapping("/{id}")
    public RouteWithStopsAndHoursDTO updateRoute(@PathVariable UUID id, @RequestBody RouteWithStopsAndHoursDTO dto) {
        return routeService.updateRoute(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoute(@PathVariable UUID id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok().build();
    }
}
