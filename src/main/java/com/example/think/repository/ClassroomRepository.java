package com.example.think.repository;

import com.example.think.entity.Classroom;
import com.example.think.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findByProfessor(User professor);
    List<Classroom> findByStudentsContaining(User student);
    Optional<Classroom> findByClassCode(String classCode);
} 