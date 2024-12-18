package com.example.think.service;

import com.example.think.entity.Classroom;
import com.example.think.entity.User;
import com.example.think.entity.Assignment;
import com.example.think.entity.AssignmentSubmission;
import com.example.think.entity.UserRole;
import com.example.think.dto.AssignmentDto;
import com.example.think.repository.ClassroomRepository;
import com.example.think.repository.AssignmentRepository;
import com.example.think.repository.AssignmentSubmissionRepository;
import com.example.think.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClassroomService {
    private static final Logger logger = LoggerFactory.getLogger(ClassroomService.class);
    
    @Autowired
    private ClassroomRepository classroomRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private AssignmentSubmissionRepository submissionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Classroom createClassroom(Classroom classroom) {
        logger.debug("Creating new classroom: {}", classroom.getName());
        try {
            String classCode = generateClassCode();
            logger.debug("Generated class code: {}", classCode);
            classroom.setClassCode(classCode);
            
            Classroom savedClassroom = classroomRepository.save(classroom);
            logger.info("Successfully created classroom with ID: {}", savedClassroom.getId());
            return savedClassroom;
        } catch (Exception e) {
            logger.error("Error creating classroom", e);
            throw new RuntimeException("강의실 생성 중 오류가 발생했습니다", e);
        }
    }
    
    public List<Classroom> getMyClassrooms(User professor) {
        logger.debug("Getting classrooms for professor: {}", professor.getStudentId());
        try {
            List<Classroom> classrooms = classroomRepository.findByProfessor(professor);
            logger.info("Found {} classrooms for professor", classrooms.size());
            return classrooms;
        } catch (Exception e) {
            logger.error("Error getting professor's classrooms", e);
            throw new RuntimeException("교수의 강의실 목록을 가져는 중 오류가 발생했습니다", e);
        }
    }
    
    public List<Classroom> getEnrolledClassrooms(User student) {
        logger.debug("Getting enrolled classrooms for student: {}", student.getStudentId());
        try {
            List<Classroom> classrooms = classroomRepository.findByStudentsContaining(student);
            logger.info("Found {} enrolled classrooms for student", classrooms.size());
            return classrooms;
        } catch (Exception e) {
            logger.error("Error getting student's enrolled classrooms", e);
            throw new RuntimeException("학생의 수강 강의실 목록을 가져오는 중 오류가 발생했습니다", e);
        }
    }
    
    public Classroom getClassroomById(Long id) {
        return classroomRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("강의실을 찾을 수 없습니다."));
    }
    
    public Classroom findByClassCode(String code) {
        return classroomRepository.findByClassCode(code)
            .orElseThrow(() -> new RuntimeException("유효하지 않은 초대 코드입니다."));
    }
    
    public void enrollStudent(String classCode, User student) {
        Classroom classroom = findByClassCode(classCode);
        if (classroom.getStudents().contains(student)) {
            throw new RuntimeException("이 등록된 학생입니다.");
        }
        classroom.getStudents().add(student);
        classroomRepository.save(classroom);
    }
    
    private String generateClassCode() {
        // 랜덤한 6자리 코드 생성
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    public Assignment createAssignment(Long classroomId, AssignmentDto assignmentDto, User professor) {
        logger.debug("Creating assignment for classroom: {} by professor: {}", classroomId, professor.getStudentId());
        
        try {
            Classroom classroom = getClassroomById(classroomId);
            
            // 교수 권한 및 해당 강의실의 담당 교수인지 확인
            if (professor.getRole() != UserRole.PROFESSOR) {
                logger.error("Non-professor user attempted to create assignment: {}", professor.getStudentId());
                throw new RuntimeException("교수만 과제를 생성할 수 있습니다.");
            }
            
            if (!classroom.getProfessor().getId().equals(professor.getId())) {
                logger.error("Professor {} attempted to create assignment for another professor's classroom", 
                    professor.getStudentId());
                throw new RuntimeException("해당 강의실의 담당 교수만 과제를 생성할 수 있습니다.");
            }

            Assignment assignment = new Assignment();
            assignment.setTitle(assignmentDto.getTitle());
            assignment.setDescription(assignmentDto.getDescription());
            assignment.setDueDate(assignmentDto.getDueDate());
            assignment.setClassroom(classroom);

            Assignment savedAssignment = assignmentRepository.save(assignment);
            logger.info("Successfully created assignment: {} for classroom: {}", 
                savedAssignment.getId(), classroomId);
            
            return savedAssignment;
        } catch (Exception e) {
            logger.error("Error creating assignment", e);
            throw new RuntimeException("과제 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public List<Assignment> getAssignments(Long classroomId) {
        return assignmentRepository.findByClassroomId(classroomId);
    }

    public AssignmentSubmission submitAssignment(Long assignmentId, User student, String imageData) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("과제를 찾을 수 없습니다."));

        if (assignment.getDueDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("제출 기한이 지났습니다.");
        }

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setImageData(imageData);

        return submissionRepository.save(submission);
    }

    public List<AssignmentSubmission> getAssignmentSubmissions(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new RuntimeException("과제를 찾을 수 없습니다."));
        return submissionRepository.findByAssignment_Id(assignmentId);
    }

    public List<AssignmentSubmission> getStudentSubmissions(Long studentId) {
        return submissionRepository.findByStudent_Id(studentId);
    }

    public void deleteClassroom(Long classroomId, User professor) {
        logger.debug("Attempting to delete classroom {} by professor {}", classroomId, professor.getStudentId());
        
        Classroom classroom = classroomRepository.findById(classroomId)
            .orElseThrow(() -> new RuntimeException("강의실을 찾을 수 없습니다."));
        
        // 교수 권한 확인
        if (professor.getRole() != UserRole.PROFESSOR) {
            logger.error("Non-professor user attempted to delete classroom: {}", professor.getStudentId());
            throw new RuntimeException("교수만 강의실을 삭제할 수 있습니다.");
        }
        
        // 해당 강의실의 담당 교수인지 확인
        if (!classroom.getProfessor().getId().equals(professor.getId())) {
            logger.error("Professor {} attempted to delete another professor's classroom {}", 
                professor.getStudentId(), classroomId);
            throw new RuntimeException("해당 강의실의 담당 교수만 삭제할 수 있습니다.");
        }
        
        try {
            // 강의실에 속한 과제들 먼저 삭제
            List<Assignment> assignments = assignmentRepository.findByClassroomId(classroomId);
            for (Assignment assignment : assignments) {
                // 과제에 속한 제출물들 삭제
                List<AssignmentSubmission> submissions = 
                    submissionRepository.findByAssignment_Id(assignment.getId());
                submissionRepository.deleteAll(submissions);
            }
            assignmentRepository.deleteAll(assignments);
            
            // 강의실 삭제
            classroomRepository.delete(classroom);
            logger.info("Successfully deleted classroom {} by professor {}", 
                classroomId, professor.getStudentId());
        } catch (Exception e) {
            logger.error("Error deleting classroom {}: {}", classroomId, e.getMessage());
            throw new RuntimeException("강의실 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public void deleteSubmission(Long submissionId, User student) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("제출물을 찾을 수 없습니다."));
        
        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("자신의 제출물만 삭제할 수 있습니다.");
        }
        
        // 제출 기한이 지났는지 확인
        if (submission.getAssignment().getDueDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("제출 기한이 지난 과제는 삭제할 수 없습니다.");
        }
        
        submissionRepository.delete(submission);
    }

    public AssignmentSubmission updateSubmission(Long submissionId, User student, String imageData) {
        logger.debug("Attempting to update submission ID {} by student {}", 
            submissionId, student.getStudentId());
        
        // 먼저 제출물 목록을 조회하여 로깅
        List<AssignmentSubmission> allSubmissions = submissionRepository.findByStudent_Id(student.getId());
        logger.debug("Available submissions for student: {}", 
            allSubmissions.stream()
                .map(s -> String.format("ID: %d, Assignment: %s", s.getId(), s.getAssignment().getTitle()))
                .collect(Collectors.joining(", ")));
        
        // 제출물 존재 여부 확인
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> {
                logger.error("Submission not found: {}. Available submission IDs: {}", 
                    submissionId, 
                    allSubmissions.stream()
                        .map(s -> s.getId().toString())
                        .collect(Collectors.joining(", ")));
                return new RuntimeException("제출물을 찾을 수 없습니다. (ID: " + submissionId + ")");
            });
        
        // 본인의 제출물인지 확인
        if (!submission.getStudent().getId().equals(student.getId())) {
            logger.error("Student {} attempted to modify submission {} owned by student {}", 
                student.getStudentId(), submissionId, submission.getStudent().getStudentId());
            throw new RuntimeException("자신의 제출물만 수정할 수 있습니다.");
        }
        
        // 과제 제출 기한 확인
        if (submission.getAssignment().getDueDate().isBefore(LocalDateTime.now())) {
            logger.warn("Assignment {} is past due date for submission {}", 
                submission.getAssignment().getId(), submissionId);
            throw new RuntimeException("제출 기한이 지났습니다.");
        }
        
        // 제출물 정보 업데이트
        submission.setImageData(imageData);
        submission.setSubmittedAt(LocalDateTime.now());
        
        logger.info("Successfully updated submission {} by student {}", 
            submissionId, student.getStudentId());
        
        return submissionRepository.save(submission);
    }

    public List<Assignment> getStudentAssignments(Long studentId) {
        return assignmentRepository.findByClassroom_Students_Id(studentId);
    }
} 