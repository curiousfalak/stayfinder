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
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private static final Logger log = Logger.getLogger(PropertyService.class.getName());

    private final PropertyRepository repository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final PropertySearchService searchService;

    @Transactional
    public PropertyResponse create(PropertyRequest req, UUID currentUserId) {
        // Owner is always the authenticated caller — never trust a client-supplied ownerId.
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + currentUserId));

        Property property = Property.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .city(req.getCity())
                .country(req.getCountry())
                .pricePerNight(req.getPricePerNight())
                .maxGuests(req.getMaxGuests())
                .owner(owner)
                .build();

        Property saved = repository.save(property);

        searchService.indexProperty(toDocument(saved));

        return toResponse(saved);
    }

    public Page<PropertyResponse> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    @Cacheable(value = "properties", key = "#id")
    public PropertyResponse getById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Property not found: " + id));
    }

    @Transactional
    @CachePut(value = "properties", key = "#id")
    public PropertyResponse update(UUID id, PropertyRequest req, UUID currentUserId) {
        Property property = findOwnedPropertyOrThrow(id, currentUserId);

        property.setTitle(req.getTitle());
        property.setDescription(req.getDescription());
        property.setCity(req.getCity());
        property.setCountry(req.getCountry());
        property.setPricePerNight(req.getPricePerNight());
        property.setMaxGuests(req.getMaxGuests());

        Property saved = repository.save(property);

        searchService.indexProperty(toDocument(saved));

        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "properties", key = "#id")
    public void delete(UUID id, UUID currentUserId) {
        Property property = findOwnedPropertyOrThrow(id, currentUserId);

        repository.delete(property);

        searchService.deleteProperty(id.toString());
    }

    @Transactional
    public PropertyResponse uploadImage(UUID id, MultipartFile file, UUID currentUserId) {
        Property property = findOwnedPropertyOrThrow(id, currentUserId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        try {
            String url = minioService.uploadFile(file);

            if (property.getImageUrls() == null) {
                property.setImageUrls(new ArrayList<>());
            }
            property.getImageUrls().add(url);

            Property saved = repository.save(property);

            searchService.indexProperty(toDocument(saved));

            return toResponse(saved);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a property and verifies the current user owns it.
     * Throws ResourceNotFoundException if missing, AccessDeniedException if not owned.
     */
    private Property findOwnedPropertyOrThrow(UUID propertyId, UUID currentUserId) {
        Property property = repository.findById(propertyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Property not found: " + propertyId));

        if (!property.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not own this property");
        }

        return property;
    }

    private PropertyResponse toResponse(Property property) {
        List<String> images = new ArrayList<>();
        if (property.getImageUrls() != null) {
            images.addAll(property.getImageUrls());
        }

        return PropertyResponse.builder()
                .id(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .city(property.getCity())
                .country(property.getCountry())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .createdAt(property.getCreatedAt())
                .imageUrls(images)
                .build();
    }

    private PropertyDocument toDocument(Property property) {
        List<String> images = new ArrayList<>();
        if (property.getImageUrls() != null) {
            images.addAll(property.getImageUrls());
        }

        return PropertyDocument.builder()
                .id(property.getId().toString())
                .title(property.getTitle())
                .description(property.getDescription())
                .city(property.getCity())
                .country(property.getCountry())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .avgRating(0.0)
                .imageUrls(images)
                .build();
    }
}