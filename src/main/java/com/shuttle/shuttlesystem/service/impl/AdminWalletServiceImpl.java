package com.shuttle.shuttlesystem.service.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.AdminWalletAllocateRequestDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletAllocateResponseDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletAnalyticsDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletBulkAllocateRequestDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletSummaryDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletTransactionDTO;
import com.shuttle.shuttlesystem.model.Student;
import com.shuttle.shuttlesystem.model.TransactionType;
import com.shuttle.shuttlesystem.model.User;
import com.shuttle.shuttlesystem.model.WalletTransaction;
import com.shuttle.shuttlesystem.repository.StudentRepository;
import com.shuttle.shuttlesystem.repository.TransactionTypeRepository;
import com.shuttle.shuttlesystem.repository.UserRepository;
import com.shuttle.shuttlesystem.repository.WalletTransactionRepository;
import com.shuttle.shuttlesystem.service.AdminWalletService;

@Service
public class AdminWalletServiceImpl implements AdminWalletService {
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private TransactionTypeRepository transactionTypeRepository;

    @Override
    public List<AdminWalletSummaryDTO> getAllStudentWallets() {
        
        List<Student> students = studentRepository.findAll();
        List<AdminWalletSummaryDTO> result = new ArrayList<>();
        for (Student s : students) {
            AdminWalletSummaryDTO dto = new AdminWalletSummaryDTO();
            dto.studentId = s.getStudentId(); // Use studentId (string) instead of UUID
            dto.walletBalance = s.getWalletBalance();
            if (s.getUser() != null) {
                dto.name = s.getUser().getName();
                dto.email = s.getUser().getEmail();
            }
            List<WalletTransaction> txs = walletTransactionRepository.findByStudent_Id(s.getId());
            dto.totalAllocated = txs.stream().filter(t -> t.getAmount() > 0).mapToInt(WalletTransaction::getAmount).sum();
            dto.totalSpent = txs.stream().filter(t -> t.getAmount() < 0).mapToInt(t -> -t.getAmount()).sum();
            dto.lastTransaction = txs.stream().map(WalletTransaction::getCreatedAt).max(Date::compareTo).orElse(null);
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<AdminWalletTransactionDTO> getAllWalletTransactions() {
        List<WalletTransaction> txs = walletTransactionRepository.findAllByOrderByCreatedAtDesc();
        List<AdminWalletTransactionDTO> result = new ArrayList<>();
        for (WalletTransaction wt : txs) {
            AdminWalletTransactionDTO dto = new AdminWalletTransactionDTO();
            dto.id = wt.getId();
            dto.studentId = wt.getStudent() != null ? wt.getStudent().getStudentId() : null; // Use studentId (string) instead of UUID
            dto.studentName = wt.getStudent() != null && wt.getStudent().getUser() != null ? wt.getStudent().getUser().getName() : null;
            dto.type = wt.getTransactionType() != null ? wt.getTransactionType().getName() : null;
            dto.amount = wt.getAmount();
            dto.description = wt.getDescription();
            dto.reference = wt.getReferenceId();
            dto.bookingId = wt.getBooking() != null ? wt.getBooking().getId() : null;
            dto.processedBy = wt.getProcessedBy() != null ? wt.getProcessedBy().getId() : null;
            dto.processedByName = wt.getProcessedBy() != null ? wt.getProcessedBy().getName() : null;
            dto.status = "completed"; // For now, assume all are completed
            dto.createdAt = wt.getCreatedAt();
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional
    public AdminWalletAllocateResponseDTO allocatePoints(AdminWalletAllocateRequestDTO req) {
        AdminWalletAllocateResponseDTO resp = new AdminWalletAllocateResponseDTO();
        try {
            // Find student by studentCode
            Optional<Student> studentOpt = studentRepository.findByStudentId(req.studentCode);
            if (studentOpt.isEmpty()) {
                System.err.println("Student not found with code: " + req.studentCode);
                resp.success = false;
                resp.walletBalance = 0;
                resp.updated = 0;
                return resp;
            }
            Student student = studentOpt.get();
            // Find transaction type
            Optional<TransactionType> typeOpt = transactionTypeRepository.findByName(req.type);
            if (typeOpt.isEmpty()) {
                System.err.println("Transaction type not found: " + req.type);
                resp.success = false;
                resp.walletBalance = student.getWalletBalance();
                resp.updated = 0;
                return resp;
            }
            TransactionType type = typeOpt.get();
            // Find the user who processed this (if provided)
            User processedByUser = null;
            if (req.processedBy != null && !req.processedBy.isEmpty()) {
                try {
                    java.util.UUID processedByUuid = java.util.UUID.fromString(req.processedBy);
                    processedByUser = userRepository.findById(processedByUuid).orElse(null);
                } catch (Exception e) {
                    processedByUser = null;
                }
            }
            // Create wallet transaction
            WalletTransaction tx = new WalletTransaction();
            tx.setStudent(student);
            tx.setTransactionType(type);
            tx.setAmount(req.amount);
            tx.setDescription(req.description);
            tx.setReferenceId(req.reference);
            tx.setProcessedBy(processedByUser);
            
            // Save transaction
            walletTransactionRepository.save(tx);
            
            // Update student wallet balance
            int newBalance = student.getWalletBalance() + req.amount;
            student.setWalletBalance(newBalance);
            studentRepository.save(student);
            
            // Return success response
            resp.success = true;
            resp.walletBalance = newBalance;
            resp.updated = 1;
            
            System.out.println("Successfully allocated " + req.amount + " points to student " + student.getStudentId() + ". New balance: " + newBalance);
            
        } catch (Exception e) {
            System.err.println("Error in allocatePoints: " + e.getMessage());
            e.printStackTrace();
            resp.success = false;
            resp.walletBalance = 0;
            resp.updated = 0;
        }
        
        return resp;
    }

    @Override
    @Transactional
    public AdminWalletAllocateResponseDTO bulkAllocatePoints(AdminWalletBulkAllocateRequestDTO req) {
        AdminWalletAllocateResponseDTO resp = new AdminWalletAllocateResponseDTO();
        TransactionType type = transactionTypeRepository.findByName(req.type).orElseThrow();
        int updated = 0;
        for (UUID studentId : req.studentIds) {
            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) continue;
            Student student = studentOpt.get();
            WalletTransaction tx = new WalletTransaction();
            tx.setStudent(student);
            tx.setTransactionType(type);
            tx.setAmount(req.amount);
            tx.setDescription(req.description);
            tx.setReferenceId(req.reference);
            tx.setProcessedBy(userRepository.findById(req.processedBy).orElse(null));
            walletTransactionRepository.save(tx);
            student.setWalletBalance(student.getWalletBalance() + req.amount);
            studentRepository.save(student);
            updated++;
        }
        resp.success = true;
        resp.updated = updated;
        return resp;
    }

    @Override
    public AdminWalletAnalyticsDTO getWalletAnalytics() {
        AdminWalletAnalyticsDTO dto = new AdminWalletAnalyticsDTO();
        List<Student> students = studentRepository.findAll();
        dto.totalBalance = students.stream().mapToInt(Student::getWalletBalance).sum();
        dto.totalStudents = students.size();
        dto.averageBalance = students.isEmpty() ? 0 : dto.totalBalance / students.size();
        // Monthly allocated/spent
        ZonedDateTime firstOfMonth = ZonedDateTime.now(ZoneId.systemDefault()).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        Date firstOfMonthDate = Date.from(firstOfMonth.toInstant());
        List<WalletTransaction> txs = walletTransactionRepository.findByCreatedAtAfter(firstOfMonthDate);
        dto.monthlyAllocated = txs.stream().filter(t -> t.getAmount() > 0).mapToInt(WalletTransaction::getAmount).sum();
        dto.monthlySpent = txs.stream().filter(t -> t.getAmount() < 0).mapToInt(t -> -t.getAmount()).sum();
        dto.lowBalanceCount = (int) students.stream().filter(s -> s.getWalletBalance() < 1000).count();
        return dto;
    }
}
