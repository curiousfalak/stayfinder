package com.example.stayfinder.backend.dto;



import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID guestId;
    private String guestName;
    private UUID propertyId;
    private String propertyTitle;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}