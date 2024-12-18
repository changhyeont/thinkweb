package com.example.think.service;

import com.example.think.entity.User;
import com.example.think.repository.UserRepository;
import com.example.think.security.UserPrincipal;
import com.example.think.dto.UserDto;
import com.example.think.dto.PasswordChangeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Primary
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Override
    public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
        logger.debug("Loading user by studentId: {}", studentId);
        
        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> {
                logger.debug("User not found with studentId: {}", studentId);
                return new UsernameNotFoundException("User not found with studentId: " + studentId);
            });
        
        logger.debug("User found: {}", user.getName());
        return UserPrincipal.create(user);
    }

    public UserDto getUserInfo(User user) {
        logger.debug("Converting user info to DTO for: {}", user.getStudentId());
        return convertToDto(user);
    }

    public UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setStudentId(user.getStudentId());
        dto.setDepartment(user.getDepartment());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        return dto;
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeDto passwordChangeDto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(passwordChangeDto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 유효성 검사 (예: 최소 길이)
        if (passwordChangeDto.getNewPassword().length() < 8) {
            throw new RuntimeException("새 비밀번호는 최소 8자 이상이어야 합니다.");
        }

        // 새 비밀번호 암호화 및 저장
        user.setPassword(passwordEncoder.encode(passwordChangeDto.getNewPassword()));
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", user.getStudentId());
    }
} 