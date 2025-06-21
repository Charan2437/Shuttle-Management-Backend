package com.shuttle.shuttlesystem.service.impl;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.StudentWalletAnalyticsDTO;
import com.shuttle.shuttlesystem.dto.StudentWalletBalanceDTO;
import com.shuttle.shuttlesystem.dto.StudentWalletTransactionDTO;
import com.shuttle.shuttlesystem.model.Student;
import com.shuttle.shuttlesystem.model.TransactionType;
import com.shuttle.shuttlesystem.model.WalletTransaction;
import com.shuttle.shuttlesystem.repository.BookingRepository;
import com.shuttle.shuttlesystem.repository.RouteRepository;
import com.shuttle.shuttlesystem.repository.StudentRepository;
import com.shuttle.shuttlesystem.repository.TransactionTypeRepository;
import com.shuttle.shuttlesystem.repository.WalletTransactionRepository;
import com.shuttle.shuttlesystem.service.StudentWalletService;

@Service
public class StudentWalletServiceImpl implements StudentWalletService {
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;
    @Autowired
    private TransactionTypeRepository transactionTypeRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private RouteRepository routeRepository;

    @Override
    public StudentWalletBalanceDTO getWalletBalance(String email) {
        Student student = studentRepository.findByUserEmail(email).orElseThrow();
        List<WalletTransaction> txs = walletTransactionRepository.findByStudent_Id(student.getId());
        int totalAllocated = txs.stream().filter(t -> t.getAmount() > 0).mapToInt(WalletTransaction::getAmount).sum();
        int totalSpent = txs.stream().filter(t -> t.getAmount() < 0).mapToInt(t -> -t.getAmount()).sum();
        Date lastTransaction = txs.stream().map(WalletTransaction::getCreatedAt).max(Date::compareTo).orElse(null);
        StudentWalletBalanceDTO dto = new StudentWalletBalanceDTO();
        dto.studentId = student.getStudentId();
        dto.walletBalance = student.getWalletBalance() != null ? student.getWalletBalance() : 0;
        dto.totalAllocated = totalAllocated;
        dto.totalSpent = totalSpent;
        dto.lastTransaction = lastTransaction;
        return dto;
    }

    @Override
    public Page<StudentWalletTransactionDTO> getTransactionHistory(String email, Pageable pageable) {
        Student student = studentRepository.findByUserEmail(email).orElseThrow();
        List<WalletTransaction> txs = walletTransactionRepository.findByStudent_Id(student.getId());
        List<StudentWalletTransactionDTO> dtos = txs.stream()
            .sorted(Comparator.comparing(WalletTransaction::getCreatedAt).reversed())
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .map(t -> {
                StudentWalletTransactionDTO dto = new StudentWalletTransactionDTO();
                dto.setId(t.getId().getMostSignificantBits());
                dto.setAmount(new java.math.BigDecimal(t.getAmount()));
                dto.setType(t.getAmount() > 0 ? "credit" : "debit");
                dto.setDescription(t.getDescription());
                dto.setTimestamp(new java.sql.Timestamp(t.getCreatedAt().getTime()).toLocalDateTime());
                return dto;
            })
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, txs.size());
    }

    @Override
    public StudentWalletAnalyticsDTO getWalletAnalytics(String email) {
        try {
        Student student = studentRepository.findByUserEmail(email).orElseThrow();
            UUID studentId = student.getId();
            List<WalletTransaction> txs = walletTransactionRepository.findByStudent_Id(studentId);
            
            // Monthly credits
            List<Map<String, Object>> monthlyCredits = txs.stream().filter(t -> t.getAmount() > 0)
                .collect(Collectors.groupingBy(
                    t -> t.getCreatedAt().toInstant().toString().substring(0,7), // YYYY-MM
                    Collectors.summingInt(WalletTransaction::getAmount)))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", e.getKey());
                    map.put("amount", e.getValue());
                    return map;
                })
                .sorted((a, b) -> ((String)b.get("month")).compareTo((String)a.get("month")))
                .collect(Collectors.toList());
            
            // Monthly debits
            List<Map<String, Object>> monthlyDebits = txs.stream().filter(t -> t.getAmount() < 0)
                .collect(Collectors.groupingBy(
                    t -> t.getCreatedAt().toInstant().toString().substring(0,7),
                    Collectors.summingInt(t -> -t.getAmount())))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("month", e.getKey());
                    map.put("amount", e.getValue());
                    return map;
                })
                .sorted((a, b) -> ((String)b.get("month")).compareTo((String)a.get("month")))
                .collect(Collectors.toList());
            
            // Total trips
            List<com.shuttle.shuttlesystem.model.Booking> bookings = bookingRepository.findByStudentId(studentId);
            int totalTrips = bookings.size();
            
            // Avg cost per trip
            int totalSpent = txs.stream().filter(t -> t.getAmount() < 0).mapToInt(t -> -t.getAmount()).sum();
            int avgCostPerTrip = totalTrips > 0 ? Math.round((float)totalSpent / totalTrips) : 0;
            
            // Most used route
            String mostUsedRoute = null;
            if (!bookings.isEmpty()) {
                Map<UUID, Long> routeCounts = bookings.stream()
                    .filter(b -> b.getRoute() != null)
                    .collect(Collectors.groupingBy(
                        b -> b.getRoute().getId(), Collectors.counting()));
                if (!routeCounts.isEmpty()) {
                    UUID mostUsedRouteId = routeCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
                    mostUsedRoute = routeRepository.findById(mostUsedRouteId).map(r -> r.getName()).orElse(null);
                }
            }
            
            // Peak usage time
            String peakUsageTime = null;
            if (!bookings.isEmpty()) {
                Map<Integer, Long> hourCounts = bookings.stream()
                    .filter(b -> b.getScheduledTime() != null)
                    .collect(Collectors.groupingBy(
                        b -> b.getScheduledTime().atZone(java.time.ZoneId.systemDefault()).getHour(), Collectors.counting()));
                if (!hourCounts.isEmpty()) {
                    int peakHour = hourCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
                    peakUsageTime = String.format("%02d:00 - %02d:00", peakHour, (peakHour+1)%24);
                }
            }
            
            // Points saved (set to 0 for now)
            int pointsSaved = 0;
            
        StudentWalletAnalyticsDTO dto = new StudentWalletAnalyticsDTO();
            dto.monthlyCredits = monthlyCredits;
            dto.monthlyDebits = monthlyDebits;
            dto.totalTrips = totalTrips;
            dto.avgCostPerTrip = avgCostPerTrip;
            dto.mostUsedRoute = mostUsedRoute;
            dto.peakUsageTime = peakUsageTime;
            dto.pointsSaved = pointsSaved;
        return dto;
        } catch (Exception e) {
            System.err.println("Error in getWalletAnalytics: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> rechargeWallet(String email, int amount, String razorpayPaymentId) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("Starting recharge for email: " + email + ", amount: " + amount + ", paymentId: " + razorpayPaymentId);
            
            Student student = studentRepository.findByUserEmail(email).orElseThrow(() -> {
                System.out.println("Student not found for email: " + email);
                return new RuntimeException("Student not found");
            });
            
            System.out.println("Found student: " + student.getStudentId());
            
            if (amount <= 0) {
                result.put("success", false);
                result.put("error", "Amount must be greater than 0");
                return result;
            }
            if (razorpayPaymentId == null || razorpayPaymentId.isEmpty()) {
                result.put("success", false);
                result.put("error", "Payment ID is required");
                return result;
            }
            
            // Find or create TransactionType for 'credit'
            TransactionType type = transactionTypeRepository.findByName("credit")
                .orElseGet(() -> {
                    System.out.println("Creating new credit transaction type");
                    TransactionType t = new TransactionType();
                    t.setName("credit");
                    t.setDescription("Credit transaction");
                    t.setActive(true);
                    t.setCreatedAt(java.time.Instant.now());
                    return transactionTypeRepository.save(t);
                });
            
            System.out.println("Using transaction type: " + type.getName() + " with ID: " + type.getId());
            
            // Create wallet transaction
            WalletTransaction tx = new WalletTransaction();
            tx.setStudent(student);
            tx.setAmount(amount);
            tx.setTransactionType(type);
            tx.setDescription("Wallet recharge via Razorpay");
            tx.setReferenceId(razorpayPaymentId);
            tx.setCreatedAt(new java.util.Date());
            
            System.out.println("Saving wallet transaction...");
            walletTransactionRepository.save(tx);
            System.out.println("Wallet transaction saved with ID: " + tx.getId());
            
            // Update student wallet balance
            int currentBalance = student.getWalletBalance() != null ? student.getWalletBalance() : 0;
            int newBalance = currentBalance + amount;
            student.setWalletBalance(newBalance);
            
            System.out.println("Updating student balance from " + currentBalance + " to " + newBalance);
            studentRepository.save(student);
            System.out.println("Student balance updated successfully");
            
            result.put("success", true);
            result.put("walletBalance", newBalance);
            result.put("transactionId", tx.getId());
            System.out.println("Recharge completed successfully");
            return result;
        } catch (Exception e) {
            System.err.println("Recharge failed with error: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "Recharge failed. Please try again. Error: " + e.getMessage());
            return result;
        }
    }
}
