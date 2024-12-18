package com.example.think.repository;

import com.example.think.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    void deleteByExpiryDateBefore(LocalDateTime date);
} 