package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.TransferBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransferBookingRepository extends JpaRepository<TransferBooking, UUID> {
    List<TransferBooking> findByMainBookingId(UUID mainBookingId);
}
