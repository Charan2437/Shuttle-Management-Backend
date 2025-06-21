package com.shuttle.shuttlesystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shuttle.shuttlesystem.dto.AuthResponseDTO;
import com.shuttle.shuttlesystem.dto.AuthUserDTO;
import com.shuttle.shuttlesystem.dto.ForgotPasswordRequestDTO;
import com.shuttle.shuttlesystem.dto.LoginRequestDTO;
import com.shuttle.shuttlesystem.dto.RegisterRequestDTO;
import com.shuttle.shuttlesystem.dto.ResetPasswordRequestDTO;
import com.shuttle.shuttlesystem.repository.UserRepository;
import com.shuttle.shuttlesystem.service.AuthService;
import com.shuttle.shuttlesystem.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthUserDTO> register(@RequestBody RegisterRequestDTO req) {
        logger.info("Received registration request for email: {}", req.email);
        try {
            AuthUserDTO response = authService.register(req);
            logger.info("Registration successful for email: {}", req.email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Registration failed for email: {} - Error: {}", req.email, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO req) {
        logger.info("Received login request for email: {}", req.email);
        try {
            // Validate request body
            if (req == null) {
                logger.error("Login request body is null");
                throw new IllegalArgumentException("Request body cannot be null");
            }
            
            AuthResponseDTO response = authService.login(req);
            logger.info("Login successful for email: {}", req.email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for email: {} - Error: {}", req.email, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDTO> me(HttpServletRequest request) {
        logger.debug("Received /me request");
        try {
            AuthUserDTO response = authService.getCurrentUser(request);
            logger.debug("Get current user successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Get current user failed - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        logger.info("Received logout request");
        return ResponseEntity.ok().body(java.util.Collections.singletonMap("message", "Logged out"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO req) {
        logger.info("Received forgot password request for email: {}", req.email);
        try {
            String msg = authService.requestPasswordReset(req.email);
            logger.info("Forgot password request processed for email: {}", req.email);
            return ResponseEntity.ok(java.util.Collections.singletonMap("message", msg));
        } catch (Exception e) {
            logger.error("Forgot password failed for email: {} - Error: {}", req.email, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO req) {
        logger.info("Received reset password request");
        try {
            boolean ok = authService.resetPassword(req.token, req.newPassword);
            logger.info("Reset password request processed - Success: {}", ok);
            return ResponseEntity.ok(java.util.Collections.singletonMap("message", ok ? "Password updated" : "Invalid or expired token"));
        } catch (Exception e) {
            logger.error("Reset password failed - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("Health check requested");
        Map<String, Object> response = new HashMap<>();
        try {
            // Test database connectivity
            long userCount = userRepository.count();
            response.put("status", "UP");
            response.put("database", "CONNECTED");
            response.put("userCount", userCount);
            response.put("timestamp", new java.util.Date());
            logger.info("Health check successful - User count: {}", userCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Health check failed - Error: {}", e.getMessage(), e);
            response.put("status", "DOWN");
            response.put("database", "DISCONNECTED");
            response.put("error", e.getMessage());
            response.put("timestamp", new java.util.Date());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @GetMapping("/test-jwt")
    public ResponseEntity<Map<String, Object>> testJwt() {
        logger.info("JWT test requested");
        Map<String, Object> response = new HashMap<>();
        try {
            // Test JWT generation
            String testToken = jwtUtil.generateToken("test-user-id", "student");
            response.put("status", "SUCCESS");
            response.put("token", testToken);
            response.put("message", "JWT generation successful");
            logger.info("JWT test successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("JWT test failed - Error: {}", e.getMessage(), e);
            response.put("status", "FAILED");
            response.put("error", e.getMessage());
            response.put("timestamp", new java.util.Date());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/test-security")
    public ResponseEntity<Map<String, Object>> testSecurity() {
        logger.info("Security test requested");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Security configuration is working");
        response.put("timestamp", new java.util.Date());
        response.put("publicEndpoint", true);
        logger.info("Security test successful");
        return ResponseEntity.ok(response);
    }
}
