package com.shuttle.shuttlesystem.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.StudentWalletAnalyticsDTO;
import com.shuttle.shuttlesystem.dto.StudentWalletBalanceDTO;
import com.shuttle.shuttlesystem.dto.StudentWalletTransactionDTO;
import com.shuttle.shuttlesystem.service.StudentWalletService;

@RestController
@RequestMapping("/api/student/wallet")
public class StudentWalletController {
    @Autowired
    private StudentWalletService studentWalletService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public StudentWalletBalanceDTO getWalletBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return studentWalletService.getWalletBalance(userDetails.getUsername());
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('STUDENT')")
    public Page<StudentWalletTransactionDTO> getTransactionHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return studentWalletService.getTransactionHistory(userDetails.getUsername(), PageRequest.of(page, size));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentWalletAnalyticsDTO getWalletAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return studentWalletService.getWalletAnalytics(userDetails.getUsername());
    }

    @PostMapping("/recharge")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> rechargeWallet(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, Object> body) {
        try {
            System.out.println("Recharge request received");
            System.out.println("UserDetails: " + (userDetails != null ? userDetails.getUsername() : "null"));
            System.out.println("Request body: " + body);
            
            if (userDetails == null) {
                System.err.println("UserDetails is null - authentication failed");
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Authentication required"));
            }
            
            String email = userDetails.getUsername();
            Integer amount = (Integer) body.get("amount");
            String razorpayPaymentId = (String) body.get("razorpayPaymentId");
            
            System.out.println("Extracted values - email: " + email + ", amount: " + amount + ", paymentId: " + razorpayPaymentId);
            
            if (amount == null || amount <= 0 || razorpayPaymentId == null || razorpayPaymentId.isEmpty()) {
                System.err.println("Invalid request parameters");
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid amount or payment ID"));
            }
            
            Map<String, Object> result = studentWalletService.rechargeWallet(email, amount, razorpayPaymentId);
            System.out.println("Service result: " + result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Controller error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
