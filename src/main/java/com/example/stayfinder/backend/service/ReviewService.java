package com.example.stayfinder.backend.service;


import com.example.stayfinder.backend.dto.*;
import com.example.stayfinder.backend.entity.*;
import com.example.stayfinder.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertySearchService searchService;
    private final PropertySearchRepository searchRepository;

    @Transactional
    public ReviewResponse create(ReviewRequest req) {

        // Find the booking
        Booking booking = bookingRepository
                .findById(req.getBookingId())
                .orElseThrow(() -> new RuntimeException(
                        "Booking not found: " + req.getBookingId()));

        // Only CONFIRMED bookings can be reviewed
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                    "You can only review a confirmed booking");
        }

        // One review per booking
        if (reviewRepository.existsByBookingId(req.getBookingId())) {
            throw new RuntimeException(
                    "This booking has already been reviewed");
        }

        // Guest must match booking guest
        if (!booking.getGuest().getId().equals(req.getGuestId())) {
            throw new RuntimeException(
                    "You can only review your own bookings");
        }

        User guest = userRepository.findById(req.getGuestId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + req.getGuestId()));

        Review review = Review.builder()
                .booking(booking)
                .guest(guest)
                .property(booking.getProperty())
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        // Update average rating in Elasticsearch
        updateAvgRating(booking.getProperty().getId());

        return toResponse(saved);
    }

    public List<ReviewResponse> getByProperty(UUID propertyId) {
        return reviewRepository.findByPropertyId(propertyId)
                .stream().map(this::toResponse).toList();
    }

    public List<ReviewResponse> getByGuest(UUID guestId) {
        return reviewRepository.findByGuestId(guestId)
                .stream().map(this::toResponse).toList();
    }

    public Map<String, Object> getPropertyRating(UUID propertyId) {
        Double avg = reviewRepository
                .findAvgRatingByPropertyId(propertyId)
                .orElse(0.0);
        long count = reviewRepository
                .findByPropertyId(propertyId).size();
        return Map.of(
                "propertyId", propertyId,
                "averageRating", avg,
                "totalReviews", count
        );
    }

    private void updateAvgRating(UUID propertyId) {
        Double avg = reviewRepository
                .findAvgRatingByPropertyId(propertyId)
                .orElse(0.0);
        searchRepository.findById(propertyId.toString())
                .ifPresent(doc -> {
                    doc.setAvgRating(avg);
                    searchRepository.save(doc);
                });
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .guestId(r.getGuest().getId())
                .guestName(r.getGuest().getName())
                .propertyId(r.getProperty().getId())
                .propertyTitle(r.getProperty().getTitle())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}