package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, UUID> {
    Optional<TransactionType> findByName(String name);
}
