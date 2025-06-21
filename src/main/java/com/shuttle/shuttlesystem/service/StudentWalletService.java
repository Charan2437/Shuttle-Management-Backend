package com.shuttle.shuttlesystem.service;

import com.shuttle.shuttlesystem.dto.StudentWalletBalanceDTO;
import com.shuttle.shuttlesystem.dto.StudentWalletTransactionDTO;
import com.shuttle.shuttlesystem.dto.StudentWalletAnalyticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentWalletService {
    StudentWalletBalanceDTO getWalletBalance(String email);
    Page<StudentWalletTransactionDTO> getTransactionHistory(String email, Pageable pageable);
    StudentWalletAnalyticsDTO getWalletAnalytics(String email);
    java.util.Map<String, Object> rechargeWallet(String email, int amount, String razorpayPaymentId);
}
