package com.shuttle.shuttlesystem.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shuttle.shuttlesystem.model.RouteStop;

public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {
    List<RouteStop> findByRouteIdOrderByStopOrder(UUID routeId);
    void deleteByRouteId(UUID routeId);
}
