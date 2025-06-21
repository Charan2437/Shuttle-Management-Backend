package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.Shuttle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShuttleRepository extends JpaRepository<Shuttle, UUID> {
    Shuttle findByShuttleNo(String shuttleNo);
}
