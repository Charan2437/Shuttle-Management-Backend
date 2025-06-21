package com.shuttle.shuttlesystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.StudentStatsDTO;
import com.shuttle.shuttlesystem.service.StudentStatsService;

@RestController
@RequestMapping("/api/admin/students")
public class StudentStatsController {
    @Autowired
    private StudentStatsService studentStatsService;

    @GetMapping("/{studentId}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentStatsDTO getStatsForStudent(@PathVariable String studentId) {
        return studentStatsService.getStatsForStudent(studentId);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentStatsDTO> getStatsForAllStudents() {
        return studentStatsService.getStatsForAllStudents();
    }
}
