package com.shuttle.shuttlesystem.service;

import java.util.List;

import com.shuttle.shuttlesystem.dto.StudentWithUserInfoDTO;

public interface StudentService {
    List<StudentWithUserInfoDTO> getAllStudents();
    StudentWithUserInfoDTO getStudentByStudentId(String studentId);
    StudentWithUserInfoDTO createStudent(StudentWithUserInfoDTO dto);
    StudentWithUserInfoDTO updateStudent(String studentId, StudentWithUserInfoDTO dto);
    void deleteStudent(String studentId);
}
