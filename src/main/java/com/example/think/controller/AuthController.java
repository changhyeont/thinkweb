package com.example.think.controller;

import com.example.think.dto.LoginDto;
import com.example.think.dto.SignupDto;
import com.example.think.dto.UserDto;
import com.example.think.entity.User;
import com.example.think.repository.UserRepository;
import com.example.think.security.JwtTokenProvider;
import com.example.think.security.JwtAuthenticationResponse;
import com.example.think.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        logger.debug("Login attempt for student ID: {}", loginDto.getStudentId());
        ResponseEntity<?> response = authService.authenticate(loginDto);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Login successful for student ID: {}", loginDto.getStudentId());
            // 토큰 정보 로깅
            JwtAuthenticationResponse authResponse = (JwtAuthenticationResponse) response.getBody();
            if (authResponse != null) {
                logger.debug("Generated token: {}", authResponse.getAccessToken());
                // 토큰 유효성 검증
                boolean isValid = tokenProvider.validateToken(authResponse.getAccessToken());
                logger.debug("Token validation result: {}", isValid);
            }
        } else {
            logger.error("Login failed for student ID: {}", loginDto.getStudentId());
        }
        
        return response;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupDto signupDto) {
        return authService.signup(signupDto);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            logger.debug("Getting user info with token: {}", token);
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                if (tokenProvider.validateToken(jwt)) {
                    String studentId = tokenProvider.getUserIdFromJWT(jwt);
                    User user = userRepository.findByStudentId(studentId)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                    
                    // UserDto로 변환하여 반환
                    UserDto userDto = new UserDto();
                    userDto.setId(user.getId());
                    userDto.setName(user.getName());
                    userDto.setStudentId(user.getStudentId());
                    userDto.setDepartment(user.getDepartment());
                    userDto.setEmail(user.getEmail());
                    userDto.setPhoneNumber(user.getPhoneNumber());
                    userDto.setRole(user.getRole());
                    
                    logger.debug("Retrieved user info for studentId: {}", studentId);
                    return ResponseEntity.ok(userDto);
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        } catch (Exception e) {
            logger.error("Error getting user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting user info: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                authService.logout(jwt);
                return ResponseEntity.ok("로그아웃 되었습니다.");
            }
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.badRequest().body("로그아웃 처리 중 오류가 발생했습니다.");
        }
    }
} 