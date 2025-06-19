package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StopRepository extends JpaRepository<Stop, UUID> {
}
