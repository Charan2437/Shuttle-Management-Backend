package com.shuttle.shuttlesystem.controller;

import com.shuttle.shuttlesystem.dto.StudentWithUserInfoDTO;
import com.shuttle.shuttlesystem.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentWithUserInfoDTO> getAllStudents() {
        return studentService.getAllStudents();
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentWithUserInfoDTO getStudentByStudentId(@PathVariable String studentId) {
        return studentService.getStudentByStudentId(studentId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StudentWithUserInfoDTO createStudent(@RequestBody StudentWithUserInfoDTO dto) {
        return studentService.createStudent(dto);
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public StudentWithUserInfoDTO updateStudent(@PathVariable String studentId, @RequestBody StudentWithUserInfoDTO dto) {
        return studentService.updateStudent(studentId, dto);
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStudent(@PathVariable String studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.ok().body("{\"success\":true}");
    }
}
