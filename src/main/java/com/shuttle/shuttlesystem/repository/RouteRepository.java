package com.shuttle.shuttlesystem.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shuttle.shuttlesystem.model.Route;

public interface RouteRepository extends JpaRepository<Route, UUID> {
}
