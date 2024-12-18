package com.example.think.repository;

import com.example.think.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByClassroomId(Long classroomId);
    Optional<Assignment> findByIdAndClassroom_Students_Id(Long assignmentId, Long studentId);
    List<Assignment> findByClassroom_Students_Id(Long studentId);
} 