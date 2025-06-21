package com.shuttle.shuttlesystem.service;

import com.shuttle.shuttlesystem.dto.*;
import java.util.List;

public interface AdminWalletService {
    List<AdminWalletSummaryDTO> getAllStudentWallets();
    List<AdminWalletTransactionDTO> getAllWalletTransactions();
    AdminWalletAllocateResponseDTO allocatePoints(AdminWalletAllocateRequestDTO req);
    AdminWalletAllocateResponseDTO bulkAllocatePoints(AdminWalletBulkAllocateRequestDTO req);
    AdminWalletAnalyticsDTO getWalletAnalytics();
}
