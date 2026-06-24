package com.example.stayfinder.backend.service;

import com.example.stayfinder.backend.dto.*;
import com.example.stayfinder.backend.entity.*;
import com.example.stayfinder.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository repository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    public PropertyResponse create(PropertyRequest req) {
        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + req.getOwnerId()));
        Property p = Property.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .city(req.getCity())
                .country(req.getCountry())
                .pricePerNight(req.getPricePerNight())
                .maxGuests(req.getMaxGuests())
                .owner(owner)
                .build();
        return toResponse(repository.save(p));
    }

    public List<PropertyResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse).toList();
    }

    public PropertyResponse getById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException(
                        "Property not found: " + id));
    }

    public PropertyResponse update(UUID id, PropertyRequest req) {
        Property p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Property not found: " + id));
        p.setTitle(req.getTitle());
        p.setDescription(req.getDescription());
        p.setCity(req.getCity());
        p.setCountry(req.getCountry());
        p.setPricePerNight(req.getPricePerNight());
        p.setMaxGuests(req.getMaxGuests());
        return toResponse(repository.save(p));
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public PropertyResponse uploadImage(UUID id, MultipartFile file) {
        Property p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Property not found: " + id));
        try {
            String url = minioService.uploadFile(file);
            p.getImageUrls().add(url);
            return toResponse(repository.save(p));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Image upload failed: " + e.getMessage());
        }
    }

    private PropertyResponse toResponse(Property p) {
        return PropertyResponse.builder()
                .id(p.getId()).title(p.getTitle())
                .description(p.getDescription()).city(p.getCity())
                .country(p.getCountry())
                .pricePerNight(p.getPricePerNight())
                .maxGuests(p.getMaxGuests())
                .createdAt(p.getCreatedAt())
                .imageUrls(p.getImageUrls())
                .build();
    }
}