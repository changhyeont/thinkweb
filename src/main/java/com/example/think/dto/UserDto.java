package com.example.think.dto;

import com.example.think.entity.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserDto {
    private Long id;
    private String name;
    private String studentId;
    private String department;
    private String email;
    private String phoneNumber;
    private UserRole role;
} 