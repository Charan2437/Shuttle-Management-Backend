package com.shuttle.shuttlesystem.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shuttle.shuttlesystem.model.Student;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByStudentId(String studentId);
    @Query("SELECT s FROM Student s WHERE s.user.email = :email")
    Optional<Student> findByUserEmail(@Param("email") String email);
}
