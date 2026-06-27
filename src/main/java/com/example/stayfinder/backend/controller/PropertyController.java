package com.example.stayfinder.backend.controller;

import com.example.stayfinder.backend.dto.PropertyRequest;
import com.example.stayfinder.backend.dto.PropertyResponse;
import com.example.stayfinder.backend.security.CustomUserDetails;
import com.example.stayfinder.backend.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody PropertyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request, principal.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<PropertyResponse>> getAll(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody PropertyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        service.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(
                service.uploadImage(id, file, principal.getId()));
    }
}