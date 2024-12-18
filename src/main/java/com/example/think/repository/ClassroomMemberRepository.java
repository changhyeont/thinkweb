package com.example.think.repository;

import com.example.think.entity.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Long> {
    Optional<ClassroomMember> findByClassroomIdAndUserId(Long classroomId, Long userId);
} 