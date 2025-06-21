package com.shuttle.shuttlesystem.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shuttle.shuttlesystem.dto.BookingAdminDTO;

public interface BookingAdminService {
    Page<BookingAdminDTO> getAllBookings(Map<String, String> filters, Pageable pageable);
    BookingAdminDTO getBookingById(UUID bookingId);
    BookingAdminDTO updateBookingStatus(UUID bookingId, String status, UUID cancelledBy, String cancellationReason);
    void deleteBooking(UUID bookingId);
    byte[] exportBookings(String format, Map<String, String> filters);
}
