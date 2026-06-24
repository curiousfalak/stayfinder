package com.example.stayfinder.backend.dto;


import com.example.stayfinder.backend.entity.Role;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private Role role;
    private String profilePicUrl;
    private LocalDateTime createdAt;
}