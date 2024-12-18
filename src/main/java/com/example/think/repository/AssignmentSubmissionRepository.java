package com.example.think.repository;

import com.example.think.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    // 과제 ID로 제출물 찾기
    List<AssignmentSubmission> findByAssignment_Id(Long assignmentId);
    
    // 학생 ID로 제출물 찾기
    List<AssignmentSubmission> findByStudent_Id(Long studentId);
    
    // 과제 ID와 학생 ID로 제출물 찾기
    Optional<AssignmentSubmission> findByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);
}