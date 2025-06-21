package com.shuttle.shuttlesystem.service.impl;

import com.shuttle.shuttlesystem.dto.StudentStatsDTO;
import com.shuttle.shuttlesystem.model.Student;
import com.shuttle.shuttlesystem.repository.BookingRepository;
import com.shuttle.shuttlesystem.repository.StudentRepository;
import com.shuttle.shuttlesystem.repository.WalletTransactionRepository;
import com.shuttle.shuttlesystem.service.CacheService;
import com.shuttle.shuttlesystem.service.StudentStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StudentStatsServiceImpl implements StudentStatsService {
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;
    @Autowired
    private CacheService cacheService;

    @Override
    @Cacheable(value = "student-stats", key = "#studentId")
    public StudentStatsDTO getStatsForStudent(String studentId) {
        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        int totalRides = bookingRepository.countByStudentId(student.getId());
        int totalSpent = bookingRepository.sumPointsDeductedByStudentId(student.getId());
        StudentStatsDTO dto = new StudentStatsDTO();
        dto.studentId = studentId;
        dto.totalRides = totalRides;
        dto.totalSpent = totalSpent;
        return dto;
    }

    @Override
    @Cacheable(value = "student-stats", key = "'all-students'")
    public List<StudentStatsDTO> getStatsForAllStudents() {
        List<StudentStatsDTO> stats = new ArrayList<>();
        for (Student student : studentRepository.findAll()) {
            StudentStatsDTO dto = getStatsForStudent(student.getStudentId());
            stats.add(dto);
        }
        return stats;
    }
}
