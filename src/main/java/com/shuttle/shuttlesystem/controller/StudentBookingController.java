package com.shuttle.shuttlesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.BookingConfirmRequestDTO;
import com.shuttle.shuttlesystem.service.StudentBookingService;

@RestController
@RequestMapping("/api/student/bookings")
public class StudentBookingController {
    @Autowired
    private StudentBookingService studentBookingService;

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> confirmBooking(@AuthenticationPrincipal UserDetails userDetails, @RequestBody BookingConfirmRequestDTO request) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.confirmBooking(request, email));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getTripHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "status", required = false) String status
    ) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.getTripHistory(email, limit, offset, fromDate, toDate, status));
    }

    @GetMapping("/routes/frequent")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getFrequentRoutes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.getFrequentRoutes(email, limit, fromDate, toDate));
    }

    @GetMapping("/analytics/travel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getTravelAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate
    ) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.getTravelAnalytics(email, fromDate, toDate));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getUpcomingBooking(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.getUpcomingBooking(email));
    }

    @PostMapping("/mark-completed/{bookingId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> markBookingCompleted(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("bookingId") String bookingId) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.markBookingCompleted(email, bookingId));
    }

    @PostMapping("/cancel/{bookingId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> cancelBooking(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("bookingId") String bookingId) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(studentBookingService.cancelBooking(email, bookingId));
    }
}
