package com.shuttle.shuttlesystem.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shuttle.shuttlesystem.dto.AuthResponseDTO;
import com.shuttle.shuttlesystem.dto.AuthUserDTO;
import com.shuttle.shuttlesystem.dto.LoginRequestDTO;
import com.shuttle.shuttlesystem.dto.RegisterRequestDTO;
import com.shuttle.shuttlesystem.model.Student;
import com.shuttle.shuttlesystem.model.User;
import com.shuttle.shuttlesystem.repository.StudentRepository;
import com.shuttle.shuttlesystem.repository.UserRepository;
import com.shuttle.shuttlesystem.service.AuthService;
import com.shuttle.shuttlesystem.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    // In-memory token store for demo (replace with DB or cache in production)
    private final java.util.Map<String, String> resetTokens = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public AuthUserDTO register(RegisterRequestDTO req) {
        logger.info("Starting registration for email: {}", req.email);
        try {
            if (userRepository.findByEmail(req.email).isPresent()) {
                logger.warn("Registration failed: User already exists with email: {}", req.email);
                throw new RuntimeException("User already exists");
            }
            if (!req.email.endsWith("@university.edu")) {
                logger.warn("Registration failed: Invalid email domain for: {}", req.email);
                throw new IllegalArgumentException("Registration allowed only for university emails ending with @university.edu");
            }
            User user = new User();
            user.setEmail(req.email);
            user.setPasswordHash(passwordEncoder.encode(req.password));
            user.setName(req.name);
            user.setRole(req.role);
            user.setActive(true);
            userRepository.save(user);
            logger.info("User registered successfully with ID: {}", user.getId());
            
            if ("student".equalsIgnoreCase(req.role)) {
                if (req.studentId == null || req.studentId.isBlank()) {
                    logger.warn("Student registration failed: Missing student ID for email: {}", req.email);
                    throw new IllegalArgumentException("Student ID is required for student registration");
                }
                if (studentRepository.findByStudentId(req.studentId).isPresent()) {
                    logger.warn("Student registration failed: Student ID already exists: {}", req.studentId);
                    throw new IllegalArgumentException("Student ID already exists");
                }
                Student student = new Student();
                student.setUser(user);
                student.setStudentId(req.studentId);
                student.setWalletBalance(0);
                studentRepository.save(student);
                logger.info("Student record created successfully for user ID: {}", user.getId());
            }
            AuthUserDTO dto = new AuthUserDTO();
            dto.id = user.getId().toString();
            dto.email = user.getEmail();
            dto.name = user.getName();
            dto.role = user.getRole();
            logger.info("Registration completed successfully for user: {}", user.getEmail());
            return dto;
        } catch (Exception e) {
            logger.error("Registration failed for email: {} with error: {}", req.email, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO req) {
        logger.info("Starting login process for email: {}", req.email);
        try {
            // Validate input
            if (req.email == null || req.email.trim().isEmpty()) {
                logger.warn("Login failed: Email is null or empty");
                throw new IllegalArgumentException("Email is required");
            }
            if (req.password == null || req.password.trim().isEmpty()) {
                logger.warn("Login failed: Password is null or empty for email: {}", req.email);
                throw new IllegalArgumentException("Password is required");
            }
            
            logger.debug("Attempting to find user by email: {}", req.email);
            Optional<User> userOpt = userRepository.findByEmail(req.email);
            
            if (userOpt.isEmpty()) {
                logger.warn("Login failed: User not found for email: {}", req.email);
                throw new IllegalArgumentException("Invalid credentials");
            }
            
            User user = userOpt.get();
            logger.debug("User found with ID: {} and active status: {}", user.getId(), user.isActive());
            
            if (!user.isActive()) {
                logger.warn("Login failed: User account is inactive for email: {}", req.email);
                throw new IllegalArgumentException("Invalid credentials");
            }
            
            logger.debug("Verifying password for user: {}", user.getEmail());
            if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
                logger.warn("Login failed: Invalid password for email: {}", req.email);
                throw new IllegalArgumentException("Invalid credentials");
            }
            
            logger.debug("Password verified successfully, generating JWT token");
            String token;
            try {
                token = jwtUtil.generateToken(user.getId().toString(), user.getRole());
                logger.debug("JWT token generated successfully");
            } catch (Exception jwtException) {
                logger.error("JWT token generation failed: {}", jwtException.getMessage(), jwtException);
                throw new RuntimeException("JWT token generation failed: " + jwtException.getMessage(), jwtException);
            }
            
            AuthUserDTO dto = new AuthUserDTO();
            dto.id = user.getId().toString();
            dto.email = user.getEmail();
            dto.name = user.getName();
            dto.role = user.getRole();
            // Add studentId if user is a student
            if ("STUDENT".equalsIgnoreCase(user.getRole())) {
                studentRepository.findByUserEmail(user.getEmail())
                    .ifPresent(student -> dto.studentId = student.getStudentId());
            }
            
            AuthResponseDTO resp = new AuthResponseDTO();
            resp.token = token;
            resp.user = dto;
            
            logger.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());
            return resp;
            
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed with validation error: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            logger.error("Login failed with runtime error for email: {} - Error: {}", req.email, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Login failed with unexpected error for email: {} - Error: {}", req.email, e.getMessage(), e);
            throw new RuntimeException("Login failed due to internal server error: " + e.getMessage(), e);
        }
    }

    @Override
    public AuthUserDTO getCurrentUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing token");
        }
        String token = header.substring(7);
        if (!jwtUtil.validateJwtToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        String userId = jwtUtil.getUserIdFromToken(token);
        User user = userRepository.findById(java.util.UUID.fromString(userId)).orElseThrow();
        AuthUserDTO dto = new AuthUserDTO();
        dto.id = user.getId().toString();
        dto.email = user.getEmail();
        dto.name = user.getName();
        dto.role = user.getRole();
        return dto;
    }

    @Override
    public String requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            String token = java.util.UUID.randomUUID().toString();
            resetTokens.put(token, userOpt.get().getEmail());
            // TODO: Send email with reset link containing token
        }
        return "If the email exists, a reset link will be sent.";
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        String email = resetTokens.remove(token);
        if (email == null) return false;
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
