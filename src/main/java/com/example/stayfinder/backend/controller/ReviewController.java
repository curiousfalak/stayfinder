package com.example.stayfinder.backend.controller;



import com.example.stayfinder.backend.dto.*;
import com.example.stayfinder.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Only GUEST can post review
    @PostMapping
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(request));
    }

    // Get reviews for a property
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<ReviewResponse>> getByProperty(
            @PathVariable UUID propertyId) {
        return ResponseEntity.ok(
                reviewService.getByProperty(propertyId));
    }

    // Get reviews by a guest
    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<ReviewResponse>> getByGuest(
            @PathVariable UUID guestId) {
        return ResponseEntity.ok(
                reviewService.getByGuest(guestId));
    }

    // Get average rating for a property
    @GetMapping("/property/{propertyId}/rating")
    public ResponseEntity<Map<String, Object>> getRating(
            @PathVariable UUID propertyId) {
        return ResponseEntity.ok(
                reviewService.getPropertyRating(propertyId));
    }
}