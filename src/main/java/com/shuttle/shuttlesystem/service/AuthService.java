package com.shuttle.shuttlesystem.service;

import com.shuttle.shuttlesystem.dto.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthUserDTO register(RegisterRequestDTO req);
    AuthResponseDTO login(LoginRequestDTO req);
    AuthUserDTO getCurrentUser(HttpServletRequest request);
    String requestPasswordReset(String email);
    boolean resetPassword(String token, String newPassword);
}
