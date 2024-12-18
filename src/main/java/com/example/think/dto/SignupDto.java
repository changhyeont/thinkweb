package com.example.think.dto;

import lombok.Getter;
import lombok.Setter;
import com.example.think.entity.UserRole;

@Getter @Setter
public class SignupDto {
    private String name;
    private String studentId;
    private String department;
    private String email;
    private String password;
    private String phoneNumber;
    private UserRole role;
} 