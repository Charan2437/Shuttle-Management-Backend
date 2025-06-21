package com.shuttle.shuttlesystem.service.impl;

import com.shuttle.shuttlesystem.dto.BookingAdminDTO;
import com.shuttle.shuttlesystem.model.*;
import com.shuttle.shuttlesystem.repository.*;
import com.shuttle.shuttlesystem.service.BookingAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BookingAdminServiceImpl implements BookingAdminService {
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingStatusTypeRepository bookingStatusTypeRepository;
    @Autowired private TransferBookingRepository transferBookingRepository;
    @Autowired private UserRepository userRepository;

    @Override
    public Page<BookingAdminDTO> getAllBookings(Map<String, String> filters, Pageable pageable) {
        List<Booking> all = bookingRepository.findAll();
        Stream<Booking> stream = all.stream();
        if (filters.containsKey("status")) {
            String status = filters.get("status");
            stream = stream.filter(b -> b.getStatus() != null && b.getStatus().getName().equalsIgnoreCase(status));
        }
        if (filters.containsKey("studentId")) {
            String studentId = filters.get("studentId");
            stream = stream.filter(b -> b.getStudent() != null && b.getStudent().getStudentId().equals(studentId));
        }
        if (filters.containsKey("routeId")) {
            UUID routeId = UUID.fromString(filters.get("routeId"));
            stream = stream.filter(b -> b.getRoute() != null && b.getRoute().getId().equals(routeId));
        }
        // Add more filters as needed (fromDate, toDate, search)
        List<BookingAdminDTO> dtos = stream
            .sorted(Comparator.comparing(Booking::getScheduledTime).reversed())
            .skip((long) pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .map(this::toDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, all.size());
    }

    @Override
    public BookingAdminDTO getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        return toDTO(booking);
    }

    @Override
    @Transactional
    public BookingAdminDTO updateBookingStatus(UUID bookingId, String status, UUID cancelledBy, String cancellationReason) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        BookingStatusType statusType = bookingStatusTypeRepository.findByName(status).orElseThrow();
        booking.setStatus(statusType);
        if ("cancelled".equalsIgnoreCase(status)) {
            booking.setCancelledAt(java.time.Instant.now());
            if (cancelledBy != null) {
                User user = userRepository.findById(cancelledBy).orElse(null);
                booking.setCancelledBy(user);
            }
            booking.setCancellationReason(cancellationReason);
        } else {
            booking.setCancelledAt(null);
            booking.setCancelledBy(null);
            booking.setCancellationReason(null);
        }
        bookingRepository.save(booking);
        return toDTO(booking);
    }

    @Override
    @Transactional
    public void deleteBooking(UUID bookingId) {
        bookingRepository.deleteById(bookingId);
    }

    @Override
    public byte[] exportBookings(String format, Map<String, String> filters) {
        // For demo: return empty file. Implement CSV/XLSX export as needed.
        return new byte[0];
    }

    private BookingAdminDTO toDTO(Booking b) {
        BookingAdminDTO dto = new BookingAdminDTO();
        dto.id = b.getId();
        dto.studentId = b.getStudent() != null ? b.getStudent().getId() : null;
        dto.routeId = b.getRoute() != null ? b.getRoute().getId() : null;
        dto.fromStopId = b.getFromStop() != null ? b.getFromStop().getId() : null;
        dto.toStopId = b.getToStop() != null ? b.getToStop().getId() : null;
        dto.scheduledTime = b.getScheduledTime() != null ? Date.from(b.getScheduledTime()) : null;
        dto.status = b.getStatus() != null ? b.getStatus().getName() : null;
        dto.pointsDeducted = b.getPointsDeducted();
        dto.bookingReference = b.getBookingReference();
        dto.notes = b.getNotes();
        dto.createdAt = b.getCreatedAt() != null ? Date.from(b.getCreatedAt()) : null;
        if (b.getStudent() != null) {
            BookingAdminDTO.StudentInfo s = new BookingAdminDTO.StudentInfo();
            s.name = b.getStudent().getUser() != null ? b.getStudent().getUser().getName() : null;
            s.studentId = b.getStudent().getStudentId();
            dto.student = s;
        }
        if (b.getRoute() != null) {
            BookingAdminDTO.RouteInfo r = new BookingAdminDTO.RouteInfo();
            r.name = b.getRoute().getName();
            r.color = b.getRoute().getColor();
            dto.route = r;
        }
        if (b.getFromStop() != null) {
            BookingAdminDTO.StopInfo s = new BookingAdminDTO.StopInfo();
            s.name = b.getFromStop().getName();
            dto.fromStop = s;
        }
        if (b.getToStop() != null) {
            BookingAdminDTO.StopInfo s = new BookingAdminDTO.StopInfo();
            s.name = b.getToStop().getName();
            dto.toStop = s;
        }
        dto.transferBookings = transferBookingRepository.findByMainBookingId(b.getId()).stream().map(tb -> {
            BookingAdminDTO.TransferBookingDTO t = new BookingAdminDTO.TransferBookingDTO();
            t.id = tb.getId();
            t.fromStopId = tb.getFromStop() != null ? tb.getFromStop().getId() : null;
            t.toStopId = tb.getToStop() != null ? tb.getToStop().getId() : null;
            t.transferStopId = tb.getTransferStop() != null ? tb.getTransferStop().getId() : null;
            t.estimatedWaitTime = tb.getEstimatedWaitTime() != null ? tb.getEstimatedWaitTime().intValue() : 0;
            t.transferOrder = tb.getTransferOrder();
            return t;
        }).collect(Collectors.toList());
        return dto;
    }
}
