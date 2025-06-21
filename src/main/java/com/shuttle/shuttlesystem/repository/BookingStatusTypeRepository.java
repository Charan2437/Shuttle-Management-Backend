package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.BookingStatusType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingStatusTypeRepository extends JpaRepository<BookingStatusType, UUID> {
    Optional<BookingStatusType> findByName(String name);
}
