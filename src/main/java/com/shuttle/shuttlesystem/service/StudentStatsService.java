package com.shuttle.shuttlesystem.service;

import com.shuttle.shuttlesystem.dto.StudentStatsDTO;
import java.util.List;

public interface StudentStatsService {
    StudentStatsDTO getStatsForStudent(String studentId);
    List<StudentStatsDTO> getStatsForAllStudents();
}
