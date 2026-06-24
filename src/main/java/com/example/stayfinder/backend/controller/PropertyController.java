package com.example.stayfinder.backend.controller;


import com.example.stayfinder.backend.dto.PropertyRequest;
import com.example.stayfinder.backend.dto.PropertyResponse;
import com.example.stayfinder.backend.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PropertyResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PropertyRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
//        return ResponseEntity.ok(service.uploadImage(id, file));
//        System.out.println("File name: " + file.getOriginalFilename());
//        System.out.println("Content type: " + file.getContentType());
//        System.out.println("File size: " + file.getSize());
//        System.out.println("Is empty: " + file.isEmpty());
        return ResponseEntity.ok(service.uploadImage(id, file));
    }
}