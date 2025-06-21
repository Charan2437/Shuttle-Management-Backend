package com.shuttle.shuttlesystem.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.StudentWithUserInfoDTO;
import com.shuttle.shuttlesystem.model.Student;
import com.shuttle.shuttlesystem.model.User;
import com.shuttle.shuttlesystem.repository.StudentRepository;
import com.shuttle.shuttlesystem.repository.UserRepository;
import com.shuttle.shuttlesystem.service.StudentService;

@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<StudentWithUserInfoDTO> getAllStudents() {
        return studentRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public StudentWithUserInfoDTO getStudentByStudentId(String studentId) {
        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        return toDTO(student);
    }

    @Override
    @Transactional
    public StudentWithUserInfoDTO createStudent(StudentWithUserInfoDTO dto) {
        User user = new User();
        user.setName(dto.name);
        user.setEmail(dto.email);
        user.setPasswordHash(passwordEncoder.encode(dto.password));
        user.setRole("student");
        user = userRepository.save(user);
        Student student = new Student();
        student.setUser(user);
        student.setStudentId(dto.studentId);
        student.setWalletBalance(dto.walletBalance);
        student.setProfileImageUrl(dto.profileImageUrl);
        student.setPhoneNumber(dto.phoneNumber);
        student.setEmergencyContact(dto.emergencyContact);
        student.setEnrollmentDate(dto.enrollmentDate);
        student.setGraduationDate(dto.graduationDate);
        student = studentRepository.save(student);
        return toDTO(student);
    }

    @Override
    @Transactional
    public StudentWithUserInfoDTO updateStudent(String studentId, StudentWithUserInfoDTO dto) {
        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        User user = student.getUser();
        if (dto.name != null) user.setName(dto.name);
        if (dto.email != null) user.setEmail(dto.email);
        if (dto.password != null && !dto.password.isEmpty()) user.setPasswordHash(passwordEncoder.encode(dto.password));
        userRepository.save(user);
        if (dto.walletBalance != null) student.setWalletBalance(dto.walletBalance);
        if (dto.profileImageUrl != null) student.setProfileImageUrl(dto.profileImageUrl);
        if (dto.phoneNumber != null) student.setPhoneNumber(dto.phoneNumber);
        if (dto.emergencyContact != null) student.setEmergencyContact(dto.emergencyContact);
        if (dto.enrollmentDate != null) student.setEnrollmentDate(dto.enrollmentDate);
        if (dto.graduationDate != null) student.setGraduationDate(dto.graduationDate);
        studentRepository.save(student);
        return toDTO(student);
    }

    @Override
    @Transactional
    public void deleteStudent(String studentId) {
        Student student = studentRepository.findByStudentId(studentId).orElseThrow();
        studentRepository.delete(student);
        userRepository.delete(student.getUser());
    }

    private StudentWithUserInfoDTO toDTO(Student student) {
        StudentWithUserInfoDTO dto = new StudentWithUserInfoDTO();
        dto.id = student.getId();
        dto.userId = student.getUser().getId();
        dto.name = student.getUser().getName();
        dto.email = student.getUser().getEmail();
        dto.studentId = student.getStudentId();
        dto.walletBalance = student.getWalletBalance();
        dto.profileImageUrl = student.getProfileImageUrl();
        dto.phoneNumber = student.getPhoneNumber();
        dto.emergencyContact = student.getEmergencyContact();
        dto.enrollmentDate = student.getEnrollmentDate();
        dto.graduationDate = student.getGraduationDate();
        return dto;
    }
}
