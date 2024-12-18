package com.example.think.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class AssignmentDto {
    private String title;
    private String description;
    private LocalDateTime dueDate;
} 