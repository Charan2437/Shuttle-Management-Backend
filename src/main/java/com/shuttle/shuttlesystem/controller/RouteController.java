package com.shuttle.shuttlesystem.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.RoutePlanOptionDTO;
import com.shuttle.shuttlesystem.dto.RouteWithStopsAndHoursDTO;
import com.shuttle.shuttlesystem.service.RouteService;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    @Autowired
    private RouteService routeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public List<RouteWithStopsAndHoursDTO> getAllRoutes() {
        return routeService.getAllRoutes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public RouteWithStopsAndHoursDTO getRouteById(@PathVariable UUID id) {
        return routeService.getRouteById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoute(@RequestBody RouteWithStopsAndHoursDTO dto) {
        try {
            RouteWithStopsAndHoursDTO result = routeService.createRoute(dto);
            
            // Handle shuttle mapping separately if needed
            if (dto.shuttleName != null && !dto.shuttleName.isEmpty()) {
                try {
                    routeService.assignShuttleToRoute(dto.shuttleName, result.id);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to assign shuttle to route: " + e.getMessage());
                    // Don't fail the request, just log the warning
                }
            }
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("Error creating route: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Failed to create route: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RouteWithStopsAndHoursDTO updateRoute(@PathVariable UUID id, @RequestBody RouteWithStopsAndHoursDTO dto) {
        return routeService.updateRoute(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoute(@PathVariable UUID id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/optimize")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> planRoutes(
            @RequestParam("start_stop_id") UUID startStopId,
            @RequestParam("end_stop_id") UUID endStopId,
            @RequestParam(value = "departure_time", required = false) String departureTime,
            @RequestParam(value = "max_transfers", required = false, defaultValue = "2") int maxTransfers) {
        LocalDateTime depTime = null;
        if (departureTime != null && !departureTime.isEmpty()) {
            try {
                depTime = LocalDateTime.parse(departureTime);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Invalid departure_time format. Use ISO-8601.");
            }
        }
        List<RoutePlanOptionDTO> options = routeService.planRoutes(startStopId, endStopId, depTime, maxTransfers);
        return ResponseEntity.ok(options);
    }
}
