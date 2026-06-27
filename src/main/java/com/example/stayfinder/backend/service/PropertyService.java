package com.example.stayfinder.backend.service;

import com.example.stayfinder.backend.document.PropertyDocument;
import com.example.stayfinder.backend.dto.PropertyRequest;
import com.example.stayfinder.backend.dto.PropertyResponse;
import com.example.stayfinder.backend.entity.Property;
import com.example.stayfinder.backend.entity.User;
import com.example.stayfinder.backend.exception.AccessDeniedException;
import com.example.stayfinder.backend.exception.ResourceNotFoundException;
import com.example.stayfinder.backend.repository.PropertyRepository;
import com.example.stayfinder.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository repository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final PropertySearchService searchService;

    @Transactional
    public PropertyResponse create(PropertyRequest req, UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + ownerId));

        Property p = Property.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .city(req.getCity())
                .country(req.getCountry())
                .pricePerNight(req.getPricePerNight())
                .maxGuests(req.getMaxGuests())
                .owner(owner)
                .build();

        Property saved = repository.save(p);
        searchService.indexProperty(toDocument(saved));
        return toResponse(saved);
    }

    public Page<PropertyResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    @Cacheable(value = "properties", key = "#id")
    public PropertyResponse getById(UUID id) {
        System.out.println("Fetching from DATABASE: " + id);
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property not found: " + id));
    }

    @Transactional
    @CachePut(value = "properties", key = "#id")
    public PropertyResponse update(UUID id, PropertyRequest req, UUID ownerId) {
        Property p = findOwnedPropertyOrThrow(id, ownerId);

        p.setTitle(req.getTitle());
        p.setDescription(req.getDescription());
        p.setCity(req.getCity());
        p.setCountry(req.getCountry());
        p.setPricePerNight(req.getPricePerNight());
        p.setMaxGuests(req.getMaxGuests());

        Property saved = repository.save(p);
        searchService.indexProperty(toDocument(saved));
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "properties", key = "#id")
    public void delete(UUID id, UUID ownerId) {
        Property p = findOwnedPropertyOrThrow(id, ownerId);
        repository.delete(p);
        searchService.deleteProperty(id.toString());
    }

    @Transactional
    public PropertyResponse uploadImage(UUID id, MultipartFile file,
                                        UUID ownerId) {
        Property p = findOwnedPropertyOrThrow(id, ownerId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Only image uploads are allowed");
        }

        try {
            String url = minioService.uploadFile(file);
            if (p.getImageUrls() == null) {
                p.setImageUrls(new ArrayList<>());
            }
            p.getImageUrls().add(url);
            Property saved = repository.save(p);
            searchService.indexProperty(toDocument(saved));
            return toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Image upload failed: " + e.getMessage(), e);
        }
    }

    private Property findOwnedPropertyOrThrow(UUID propertyId,
                                              UUID ownerId) {
        Property p = repository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property not found: " + propertyId));
        if (!p.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "You do not own this property");
        }
        return p;
    }

    private PropertyResponse toResponse(Property p) {
        List<String> images = new ArrayList<>();
        if (p.getImageUrls() != null) {
            images.addAll(p.getImageUrls());
        }
        return PropertyResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .city(p.getCity())
                .country(p.getCountry())
                .pricePerNight(p.getPricePerNight())
                .maxGuests(p.getMaxGuests())
                .createdAt(p.getCreatedAt())
                .imageUrls(images)
                .build();
    }

    private PropertyDocument toDocument(Property p) {
        List<String> images = new ArrayList<>();
        if (p.getImageUrls() != null) {
            images.addAll(p.getImageUrls());
        }
        return PropertyDocument.builder()
                .id(p.getId().toString())
                .title(p.getTitle())
                .description(p.getDescription())
                .city(p.getCity())
                .country(p.getCountry())
                .pricePerNight(p.getPricePerNight())
                .maxGuests(p.getMaxGuests())
                .avgRating(0.0)
                .imageUrls(images)
                .build();
    }
}