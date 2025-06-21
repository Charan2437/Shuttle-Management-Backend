package com.shuttle.shuttlesystem.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.shuttle.shuttlesystem.model.User;
import com.shuttle.shuttlesystem.repository.UserRepository;
import com.shuttle.shuttlesystem.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String header = request.getHeader("Authorization");
            
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                
                if (jwtUtil.validateJwtToken(token)) {
                    String userId = jwtUtil.getUserIdFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);
                    
                    // Find user in database
                    User user = userRepository.findById(java.util.UUID.fromString(userId)).orElse(null);
                    
                    if (user != null && user.isActive()) {
                        // Create authorities list with ROLE_ prefix for Spring Security
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        
                        // Create UserDetails object
                        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                                .username(user.getEmail())
                                .password("") // We don't need the password for JWT authentication
                                .authorities(authorities)
                                .accountExpired(false)
                                .accountLocked(false)
                                .credentialsExpired(false)
                                .disabled(!user.isActive())
                                .build();
                        
                        // Create authentication token
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        
                        // Set authentication in context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        logger.debug("User authenticated: {} with role: {}", user.getEmail(), role);
                    } else {
                        logger.warn("User not found or inactive: {}", userId);
                    }
                } else {
                    logger.warn("Invalid JWT token");
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
} 