package com.shuttle.shuttlesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.service.AdminAnalyticsService;
import com.shuttle.shuttlesystem.dto.RouteHourlyBookingsByNameRequestDTO;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        return ResponseEntity.ok(adminAnalyticsService.getOverview(fromDate, toDate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/routes")
    public ResponseEntity<?> getRouteAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        return ResponseEntity.ok(adminAnalyticsService.getRouteAnalytics(fromDate, toDate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/students")
    public ResponseEntity<?> getStudentAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        return ResponseEntity.ok(adminAnalyticsService.getStudentAnalytics(fromDate, toDate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/routes/hourly-bookings")
    public ResponseEntity<?> getRouteHourlyBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        return ResponseEntity.ok(adminAnalyticsService.getRouteHourlyBookings(fromDate, toDate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/routes/hourly-bookings/by-name")
    public ResponseEntity<?> getRouteHourlyBookingsByName(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RouteHourlyBookingsByNameRequestDTO request
    ) {
        return ResponseEntity.ok(adminAnalyticsService.getRouteHourlyBookingsByName(
            request.getRouteName(), request.getFromDate(), request.getToDate()
        ));
    }
}
