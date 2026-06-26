package com.example.stayfinder.backend.dto;
import com.example.stayfinder.backend.entity.BookingStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonDeserialize
public class BookingResponse {
    private UUID id;
    private UUID propertyId;
    private String propertyTitle;
    private UUID guestId;
    private String guestName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}