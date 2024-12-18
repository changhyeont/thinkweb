package com.example.think.controller;

import com.example.think.entity.Classroom;
import com.example.think.entity.User;
import com.example.think.entity.Assignment;
import com.example.think.entity.AssignmentSubmission;
import com.example.think.entity.UserRole;
import com.example.think.dto.AssignmentDto;
import com.example.think.service.ClassroomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classroom")
@CrossOrigin(
    origins = "*",
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
            RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class ClassroomController {
    private static final Logger logger = LoggerFactory.getLogger(ClassroomController.class);

    @Autowired
    private ClassroomService classroomService;

    @GetMapping("/list")
    public ResponseEntity<?> getClassroomList() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.error("No authentication found in SecurityContext");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
            }

            User user = (User) authentication.getPrincipal();
            logger.info("Getting classroom list for user: {}, role: {}", user.getStudentId(), user.getRole());
            
            List<Classroom> classrooms;
            if (user.getRole() == UserRole.PROFESSOR) {
                logger.debug("Fetching professor's classrooms");
                classrooms = classroomService.getMyClassrooms(user);
            } else {
                logger.debug("Fetching student's enrolled classrooms");
                classrooms = classroomService.getEnrolledClassrooms(user);
            }
            logger.info("Found {} classrooms", classrooms.size());
            return ResponseEntity.ok(classrooms);
        } catch (Exception e) {
            logger.error("Failed to get classroom list", e);
            return ResponseEntity.badRequest().body("강의실 목록을 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createClassroom(@RequestBody Classroom classroom) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            logger.info("Creating classroom: {}, professor: {}", classroom.getName(), user.getStudentId());
            classroom.setProfessor(user);
            Classroom savedClassroom = classroomService.createClassroom(classroom);
            logger.info("Created classroom with ID: {}", savedClassroom.getId());
            return ResponseEntity.ok(savedClassroom);
        } catch (Exception e) {
            logger.error("Failed to create classroom", e);
            return ResponseEntity.badRequest().body("강의실 생성에 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClassroomDetails(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            logger.debug("User {} attempting to access classroom {}", user.getStudentId(), id);
            
            Classroom classroom = classroomService.getClassroomById(id);
            
            // 교수인 경우 자신의 강의실인지 확인
            if (user.getRole() == UserRole.PROFESSOR) {
                if (classroom.getProfessor().getId().equals(user.getId())) {
                    return ResponseEntity.ok(classroom);
                }
            } 
            // 학생인 경우 등록된 학생인지 확인
            else if (user.getRole() == UserRole.STUDENT) {
                if (classroom.getStudents().stream()
                        .anyMatch(student -> student.getId().equals(user.getId()))) {
                    return ResponseEntity.ok(classroom);
                }
            }
            
            logger.warn("User {} denied access to classroom {}", user.getStudentId(), id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("이 강의실에 접근할 권한이 없습니다.");
            
        } catch (Exception e) {
            logger.error("Failed to get classroom details", e);
            return ResponseEntity.badRequest()
                .body("강의실 정보를 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/join/{code}")
    public ResponseEntity<?> joinClassroom(
        @PathVariable String code
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            classroomService.enrollStudent(code, user);
            return ResponseEntity.ok("강의실 가입이 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitAssignment(
        @PathVariable Long id,
        @RequestParam("image") String imageData
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            
            // 이미지 데이터 처리 및 저장 로직
            return ResponseEntity.ok("과제가 성공적으로 제출되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("과제 제출에 실패했습니다.");
        }
    }

    @PostMapping("/{classroomId}/assignments")
    public ResponseEntity<?> createAssignment(
        @PathVariable Long classroomId,
        @RequestBody AssignmentDto assignmentDto
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User professor = (User) authentication.getPrincipal();
            
            // 교수 권한 확인
            if (professor.getRole() != UserRole.PROFESSOR) {
                logger.error("Unauthorized attempt to create assignment by non-professor user: {}", professor.getStudentId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("교수만 과제를 생성할 수 있습니다.");
            }
            
            logger.info("Creating assignment for classroom: {} by professor: {}", classroomId, professor.getStudentId());
            Assignment assignment = classroomService.createAssignment(classroomId, assignmentDto, professor);
            return ResponseEntity.ok(assignment);
        } catch (Exception e) {
            logger.error("Failed to create assignment", e);
            return ResponseEntity.badRequest().body("과제 생성에 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{classroomId}/assignments")
    public ResponseEntity<?> getAssignments(@PathVariable Long classroomId) {
        try {
            List<Assignment> assignments = classroomService.getAssignments(classroomId);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("과제 목록을 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public ResponseEntity<?> submitAssignmentWithImage(
        @PathVariable Long assignmentId,
        @RequestBody Map<String, String> payload
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User student = (User) authentication.getPrincipal();
            
            String imageData = payload.get("imageData");
            AssignmentSubmission submission = classroomService.submitAssignment(assignmentId, student, imageData);
            return ResponseEntity.ok("과제가 성공적으로 제출되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("과제 제출에 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<?> getAssignmentSubmissions(@PathVariable Long assignmentId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User professor = (User) authentication.getPrincipal();
            
            if (professor.getRole() != UserRole.PROFESSOR) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("교수만 접근할 수 있습니다.");
            }
            
            List<AssignmentSubmission> submissions = classroomService.getAssignmentSubmissions(assignmentId);
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("제출 현황을 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<?> getMySubmissions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User student = (User) authentication.getPrincipal();
            
            List<AssignmentSubmission> submissions = classroomService.getStudentSubmissions(student.getId());
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("제출 현황을 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/{classroomId}")
    public ResponseEntity<?> deleteClassroom(@PathVariable Long classroomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User professor = (User) authentication.getPrincipal();
            
            if (professor.getRole() != UserRole.PROFESSOR) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("교수만 강의실을 삭제할 수 있습니다.");
            }
            
            classroomService.deleteClassroom(classroomId, professor);
            return ResponseEntity.ok("강의실이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("강의실 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/submissions/{submissionId}")
    public ResponseEntity<?> deleteSubmission(@PathVariable Long submissionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User student = (User) authentication.getPrincipal();
            
            classroomService.deleteSubmission(submissionId, student);
            return ResponseEntity.ok("과제 제출물이 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("과제 제출물 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/student-count")
    public ResponseEntity<?> getAllClassroomStudentCounts() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            logger.info("Getting student counts for user: {}", user.getStudentId());
            
            List<Map<String, Object>> classroomCounts = new ArrayList<>();
            List<Classroom> classrooms;
            
            if (user.getRole() == UserRole.PROFESSOR) {
                classrooms = classroomService.getMyClassrooms(user);
            } else {
                classrooms = classroomService.getEnrolledClassrooms(user);
            }
            
            for (Classroom classroom : classrooms) {
                Map<String, Object> classroomInfo = new HashMap<>();
                classroomInfo.put("classroomId", classroom.getId());
                classroomInfo.put("classroomName", classroom.getName());
                classroomInfo.put("studentCount", classroom.getStudents().size());
                classroomCounts.add(classroomInfo);
            }
            
            return ResponseEntity.ok(classroomCounts);
        } catch (Exception e) {
            logger.error("Failed to get classroom student counts", e);
            return ResponseEntity.badRequest().body("강의실 학생 수를 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/student-count")
    public ResponseEntity<?> getClassroomStudentCount(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.error("No authentication found in SecurityContext");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
            }

            User user = (User) authentication.getPrincipal();
            logger.debug("User {} requesting student count for classroom {}", user.getStudentId(), id);
            
            Classroom classroom = classroomService.getClassroomById(id);
            
            // 교수인 경우 자신의 강의실인지 확인
            if (user.getRole() == UserRole.PROFESSOR) {
                if (!classroom.getProfessor().getId().equals(user.getId())) {
                    logger.warn("Professor {} attempted to access unauthorized classroom {}", user.getStudentId(), id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("이 강의실에 접근할 권한이 없습니다.");
                }
            } 
            // 학생인 경우 등록된 학생인지 확인
            else if (user.getRole() == UserRole.STUDENT) {
                boolean isEnrolled = classroom.getStudents().stream()
                        .anyMatch(student -> student.getId().equals(user.getId()));
                if (!isEnrolled) {
                    logger.warn("Student {} attempted to access unauthorized classroom {}", user.getStudentId(), id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("이 강의실에 접근할 권한이 없습니다.");
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("classroomId", classroom.getId());
            response.put("classroomName", classroom.getName());
            response.put("studentCount", classroom.getStudents().size());
            
            logger.info("Successfully retrieved student count for classroom {}: {}", id, classroom.getStudents().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get classroom student count for classroom {}", id, e);
            return ResponseEntity.badRequest()
                .body("강의실 학생 수를 불러오는데 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<?> getClassroomStudents(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            logger.info("User {} requesting student list for classroom {}", user.getStudentId(), id);
            
            Classroom classroom = classroomService.getClassroomById(id);
            if (classroom == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 교수이거나 해당 강의실에 등록된 학생인 경우 접근 허용
            boolean hasAccess = false;
            if (user.getRole() == UserRole.PROFESSOR && classroom.getProfessor().getId().equals(user.getId())) {
                hasAccess = true;
            } else if (user.getRole() == UserRole.STUDENT && 
                    classroom.getStudents().stream()
                            .anyMatch(student -> student.getId().equals(user.getId()))) {
                hasAccess = true;
            }
            
            if (!hasAccess) {
                logger.warn("User {} denied access to classroom {} student list", user.getStudentId(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("이 강의실에 접근할 권한이 없습니다.");
            }
            
            List<Map<String, Object>> studentList = classroom.getStudents().stream()
                .map(student -> {
                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("id", student.getId());
                    studentInfo.put("studentId", student.getStudentId());
                    studentInfo.put("name", student.getName());
                    studentInfo.put("email", student.getEmail());
                    return studentInfo;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("classroomId", classroom.getId());
            response.put("classroomName", classroom.getName());
            response.put("studentCount", studentList.size());
            response.put("students", studentList);
            
            logger.info("Successfully retrieved {} students for classroom {}", studentList.size(), id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get classroom students for classroom {}", id, e);
            return ResponseEntity.badRequest()
                .body("강의실 학생 목록을 불러오는데 실패했습니: " + e.getMessage());
        }
    }

    @PutMapping("/submissions/{submissionId}")
    public ResponseEntity<?> updateSubmission(
        @PathVariable Long submissionId,
        @RequestBody Map<String, String> payload
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User student = (User) authentication.getPrincipal();
            
            logger.info("User {} attempting to update submission {}", 
                student.getStudentId(), submissionId);
            
            String imageData = payload.get("imageData");
            if (imageData == null || imageData.isEmpty()) {
                return ResponseEntity.badRequest().body("이미지 데이터가 필요합니다.");
            }
            
            AssignmentSubmission updatedSubmission = classroomService.updateSubmission(submissionId, student, imageData);
            
            logger.info("Successfully updated submission {} by user {}", 
                submissionId, student.getStudentId());
            
            return ResponseEntity.ok(updatedSubmission);
        } catch (Exception e) {
            logger.error("Failed to update submission {}", submissionId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 