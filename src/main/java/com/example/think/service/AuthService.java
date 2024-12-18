package com.example.think.service;

import com.example.think.dto.LoginDto;
import com.example.think.dto.SignupDto;
import com.example.think.entity.User;
import com.example.think.repository.UserRepository;
import com.example.think.security.JwtAuthenticationResponse;
import com.example.think.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.think.entity.BlacklistedToken;
import com.example.think.repository.BlacklistedTokenRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    public ResponseEntity<?> authenticate(LoginDto loginDto) {
        try {
            logger.debug("Attempting to authenticate user with studentId: {}", loginDto.getStudentId());
            
            User user = userRepository.findByStudentId(loginDto.getStudentId())
                .orElseThrow(() -> {
                    logger.debug("User not found with studentId: {}", loginDto.getStudentId());
                    return new UsernameNotFoundException("User not found");
                });

            if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
                logger.debug("Invalid password for studentId: {}", loginDto.getStudentId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid password");
            }

            String token = tokenProvider.generateToken(user);
            logger.debug("Generated token for user: {}", loginDto.getStudentId());
            
            logger.debug("Token validation check: {}", tokenProvider.validateToken(token));
            logger.debug("User ID from token: {}", tokenProvider.getUserIdFromJWT(token));
            
            return ResponseEntity.ok(new JwtAuthenticationResponse(token));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(e.getMessage());
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Authentication failed: " + e.getMessage());
        }
    }

    public ResponseEntity<?> signup(SignupDto signupDto) {
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        if (userRepository.existsByStudentId(signupDto.getStudentId())) {
            return ResponseEntity.badRequest().body("Student ID already exists");
        }

        try {
            User user = new User();
            user.setName(signupDto.getName());
            user.setStudentId(signupDto.getStudentId());
            user.setDepartment(signupDto.getDepartment());
            user.setEmail(signupDto.getEmail());
            user.setPassword(passwordEncoder.encode(signupDto.getPassword()));
            user.setPhoneNumber(signupDto.getPhoneNumber());
            user.setRole(signupDto.getRole());

            userRepository.save(user);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            logger.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Registration failed: " + e.getMessage());
        }
    }

    public ResponseEntity<?> getUserInfo(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                if (tokenProvider.validateToken(jwt)) {
                    String studentId = tokenProvider.getUserIdFromJWT(jwt);
                    User user = userRepository.findByStudentId(studentId)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                    
                    logger.debug("Retrieved user info for studentId: {}", studentId);
                    return ResponseEntity.ok(user);
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        } catch (Exception e) {
            logger.error("Error getting user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting user info: " + e.getMessage());
        }
    }

    public void logout(String token) {
        // 토큰이 유효한지 확인
        if (!tokenProvider.validateToken(token)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        // 토큰을 블랙리스트에 추가
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setBlacklistedAt(LocalDateTime.now());
        // 토큰 만료 시간 설정 (JWT 만료 시간과 동일하게)
        blacklistedToken.setExpiryDate(LocalDateTime.now().plusMinutes(30)); // JWT 만료 시간에 맞춰 조정

        blacklistedTokenRepository.save(blacklistedToken);
        
        // 만료된 블랙리스트 토큰 정리
        cleanupExpiredTokens();
    }

    private void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
} 