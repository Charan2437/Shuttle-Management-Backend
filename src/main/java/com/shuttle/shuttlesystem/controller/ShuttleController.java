package com.shuttle.shuttlesystem.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.ShuttleDTO;
import com.shuttle.shuttlesystem.service.ShuttleService;

@RestController
@RequestMapping("/api/shuttles")
public class ShuttleController {
    
    @Autowired
    private ShuttleService shuttleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public List<ShuttleDTO> getAllShuttles() {
        return shuttleService.getAllShuttles();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ShuttleDTO getShuttleById(@PathVariable UUID id) {
        return shuttleService.getShuttleById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ShuttleDTO createShuttle(@RequestBody ShuttleDTO dto) {
        return shuttleService.createShuttle(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ShuttleDTO updateShuttle(@PathVariable UUID id, @RequestBody ShuttleDTO dto) {
        return shuttleService.updateShuttle(id, dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShuttle(@PathVariable UUID id) {
        shuttleService.deleteShuttle(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public List<ShuttleDTO> getAvailableShuttles() {
        return shuttleService.getAvailableShuttles();
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public List<ShuttleDTO> getAssignedShuttles() {
        return shuttleService.getAssignedShuttles();
    }
} 