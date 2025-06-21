package com.shuttle.shuttlesystem.controller;

import com.shuttle.shuttlesystem.dto.BookingAdminDTO;
import com.shuttle.shuttlesystem.service.BookingAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bookings")
public class BookingAdminController {
    @Autowired
    private BookingAdminService bookingAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BookingAdminDTO> getAllBookings(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return bookingAdminService.getAllBookings(filters, PageRequest.of(page, pageSize));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingAdminDTO getBookingById(@PathVariable UUID bookingId) {
        return bookingAdminService.getBookingById(bookingId);
    }

    @PutMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable UUID bookingId,
            @RequestBody Map<String, Object> body
    ) {
        String status = (String) body.get("status");
        UUID cancelledBy = body.get("cancelledBy") != null ? UUID.fromString((String) body.get("cancelledBy")) : null;
        String cancellationReason = (String) body.get("cancellationReason");
        BookingAdminDTO updated = bookingAdminService.updateBookingStatus(bookingId, status, cancelledBy, cancellationReason);
        return ResponseEntity.ok(Map.of("success", true, "booking", updated));
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBooking(@PathVariable UUID bookingId) {
        bookingAdminService.deleteBooking(bookingId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportBookings(
            @RequestParam String format,
            @RequestParam Map<String, String> filters
    ) {
        byte[] file = bookingAdminService.exportBookings(format, filters);
        String contentType = format.equalsIgnoreCase("xlsx") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv";
        String fileName = "bookings." + format;
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .header("Content-Type", contentType)
                .body(file);
    }
}
