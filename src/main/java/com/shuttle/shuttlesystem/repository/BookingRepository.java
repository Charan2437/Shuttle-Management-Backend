package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.student.id = :studentId")
    int countByStudentId(UUID studentId);
    List<Booking> findByStudentId(UUID studentId);

    @Query("SELECT COALESCE(SUM(b.pointsDeducted), 0) FROM Booking b WHERE b.student.id = :studentId")
    int sumPointsDeductedByStudentId(UUID studentId);
}
