package com.example.stayfinder.backend.controller;

import com.example.stayfinder.backend.dto.*;
import com.example.stayfinder.backend.service.BookingHoldService;
import com.example.stayfinder.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;
    private final BookingHoldService holdService;

    // GUEST creates a booking
    @PostMapping
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    // HOST confirms a booking
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<BookingResponse> confirm(
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.confirm(id));
    }

    // HOST or GUEST can cancel
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('HOST') or hasRole('GUEST')")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    // Get single booking
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // Get all bookings for a property
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<BookingResponse>> getByProperty(
            @PathVariable UUID propertyId) {
        return ResponseEntity.ok(service.getByProperty(propertyId));
    }

    // Get all bookings for a guest
    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<BookingResponse>> getByGuest(
            @PathVariable UUID guestId) {
        return ResponseEntity.ok(service.getByGuest(guestId));
    }

    // Check availability for a property
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut) {
        boolean available = service.checkAvailability(
                propertyId, checkIn, checkOut);
        return ResponseEntity.ok(Map.of("available", available));
    }



    // Place a 30 second hold on dates
    @PostMapping("/hold")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<Map<String, Object>> placeHold(
            @RequestParam UUID propertyId,
            @RequestParam UUID guestId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut) {

        boolean held = holdService.placeHold(
                propertyId, checkIn, checkOut, guestId);

        if (!held) {
            long ttl = holdService.getHoldTtl(
                    propertyId, checkIn, checkOut);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "held", false,
                            "message", "Dates are currently held by another guest",
                            "retryAfterSeconds", ttl
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "held", true,
                "message", "Dates held for 30 seconds",
                "expiresInSeconds", 30
        ));
    }

    // Release a hold manually
    @DeleteMapping("/hold")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<Map<String, String>> releaseHold(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate checkOut) {

        holdService.releaseHold(propertyId, checkIn, checkOut);
        return ResponseEntity.ok(
                Map.of("message", "Hold released"));
    }
}
