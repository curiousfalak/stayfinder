package com.example.stayfinder.backend.dto;


import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PropertyRequest {


    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "City is required")
    private String city;

    private String country;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal pricePerNight;

    @Min(value = 1, message = "Must allow at least 1 guest")
    private int maxGuests;

}