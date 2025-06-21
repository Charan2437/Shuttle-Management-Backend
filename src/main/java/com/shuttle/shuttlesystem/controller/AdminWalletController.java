package com.shuttle.shuttlesystem.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.AdminWalletAllocateRequestDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletAllocateResponseDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletAnalyticsDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletBulkAllocateRequestDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletSummaryDTO;
import com.shuttle.shuttlesystem.dto.AdminWalletTransactionDTO;
import com.shuttle.shuttlesystem.service.AdminWalletService;

@RestController
@RequestMapping("/api/admin/wallets")
public class AdminWalletController {
    @Autowired
    private AdminWalletService adminWalletService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AdminWalletSummaryDTO> getAllStudentWallets() {
        return adminWalletService.getAllStudentWallets();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/transactions")
    public List<AdminWalletTransactionDTO> getAllWalletTransactions() {
        return adminWalletService.getAllWalletTransactions();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/allocate")
    public AdminWalletAllocateResponseDTO allocatePoints(@RequestBody AdminWalletAllocateRequestDTO request) {
        return adminWalletService.allocatePoints(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bulk-allocate")
    public AdminWalletAllocateResponseDTO bulkAllocatePoints(@RequestBody AdminWalletBulkAllocateRequestDTO request) {
        return adminWalletService.bulkAllocatePoints(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/analytics")
    public AdminWalletAnalyticsDTO getWalletAnalytics() {
        return adminWalletService.getWalletAnalytics();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/debug")
    public ResponseEntity<?> getDebugInfo() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // Check transaction types
            List<Map<String, Object>> transactionTypes = jdbcTemplate.queryForList(
                "SELECT id, name, description FROM transaction_types"
            );
            debug.put("transactionTypes", transactionTypes);
            
            // Check students
            List<Map<String, Object>> students = jdbcTemplate.queryForList(
                "SELECT s.id, s.student_id, s.wallet_balance, u.name, u.email FROM students s JOIN users u ON s.user_id = u.id LIMIT 5"
            );
            debug.put("students", students);
            
            // Check users (admins)
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, name, email, role FROM users WHERE role = 'admin' LIMIT 5"
            );
            debug.put("adminUsers", users);
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(debug);
    }
}
