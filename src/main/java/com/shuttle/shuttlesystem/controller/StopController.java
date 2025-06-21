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

import com.shuttle.shuttlesystem.model.Stop;
import com.shuttle.shuttlesystem.service.StopService;

@RestController
@RequestMapping("/api/stops")
public class StopController {
    @Autowired
    private StopService stopService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public List<Stop> getAllStops() {
        return stopService.getAllStops();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public Stop getStopById(@PathVariable UUID id) {
        return stopService.getStopById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Stop createStop(@RequestBody Stop stop) {
        return stopService.createStop(stop);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Stop updateStop(@PathVariable UUID id, @RequestBody Stop stop) {
        return stopService.updateStop(id, stop);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStop(@PathVariable UUID id) {
        stopService.deleteStop(id);
        return ResponseEntity.ok().body("{\"success\":true}");
    }
}
