package com.example.stayfinder.backend.service;


import com.example.stayfinder.backend.dto.*;
import com.example.stayfinder.backend.entity.User;
import com.example.stayfinder.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MinioService minioService;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(UserRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException(
                    "Email already registered: " + req.getEmail());
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))  // ← hash it
                .role(req.getRole())
                .build();
        return toResponse(userRepository.save(user));
    }


    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + id));
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse).toList();
    }

    public UserResponse uploadProfilePic(UUID id, MultipartFile file) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + id));
        try {
            String url = minioService.uploadFile(file);
            user.setProfilePicUrl(url);
            return toResponse(userRepository.save(user));
        } catch (Exception e) {
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId()).name(u.getName())
                .email(u.getEmail()).role(u.getRole())
                .profilePicUrl(u.getProfilePicUrl())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
