package com.example.think.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordChangeDto {
    private String currentPassword;
    private String newPassword;
} 