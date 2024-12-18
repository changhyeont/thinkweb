package com.example.think.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class ClassroomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private LocalDateTime joinedAt;
} 