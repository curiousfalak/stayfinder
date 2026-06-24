package com.example.stayfinder.backend.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyResponse {
    private UUID id;
    private String title;
    private String description;
    private String city;
    private String country;
    private BigDecimal pricePerNight;
    private int maxGuests;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
}