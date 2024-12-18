package com.example.think.repository;

import com.example.think.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentId(String studentId);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByStudentId(String studentId);
} 