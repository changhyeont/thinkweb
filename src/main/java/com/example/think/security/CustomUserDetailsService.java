package com.example.think.security;

import com.example.think.entity.User;
import com.example.think.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String studentId) throws UsernameNotFoundException {
        logger.debug("Loading user by studentId: {}", studentId);
        
        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> {
                logger.error("User not found with studentId: {}", studentId);
                return new UsernameNotFoundException("User not found with studentId: " + studentId);
            });
        
        logger.debug("Found user: {}, role: {}", user.getStudentId(), user.getRole());
        return user;
    }
} 