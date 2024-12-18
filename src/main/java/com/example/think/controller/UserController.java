package com.example.think.controller;

import com.example.think.dto.PasswordChangeDto;
import com.example.think.dto.UserDto;
import com.example.think.entity.User;
import com.example.think.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
            }

            User user = (User) authentication.getPrincipal();
            logger.debug("Fetching user info for: {}", user.getStudentId());
            
            UserDto userDto = userService.getUserInfo(user);
            return ResponseEntity.ok(userDto);
        } catch (Exception e) {
            logger.error("Failed to get user info", e);
            return ResponseEntity.badRequest().body("사용자 정보를 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeDto passwordChangeDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            userService.changePassword(user.getId(), passwordChangeDto);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (Exception e) {
            logger.error("Failed to change password", e);
            return ResponseEntity.badRequest().body("비밀번호 변경에 실패했습니다: " + e.getMessage());
        }
    }
} 