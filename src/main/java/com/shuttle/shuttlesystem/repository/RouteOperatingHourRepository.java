package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.RouteOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RouteOperatingHourRepository extends JpaRepository<RouteOperatingHour, UUID> {
    List<RouteOperatingHour> findByRouteId(UUID routeId);
    void deleteByRouteId(UUID routeId);
}
