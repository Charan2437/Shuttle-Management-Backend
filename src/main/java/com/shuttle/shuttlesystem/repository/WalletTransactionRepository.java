package com.shuttle.shuttlesystem.repository;

import com.shuttle.shuttlesystem.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WalletTransaction w WHERE w.student.id = :studentId AND w.amount < 0")
    int sumDebitsByStudentId(UUID studentId);

    List<WalletTransaction> findByStudent_Id(UUID studentId);
    List<WalletTransaction> findAllByOrderByCreatedAtDesc();
    List<WalletTransaction> findByCreatedAtAfter(Date date);
}
