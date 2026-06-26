package com.example.stayfinder.backend.service;

import com.example.stayfinder.backend.dto.*;
import com.example.stayfinder.backend.entity.*;
import com.example.stayfinder.backend.event.BookingConfirmedEvent;
import com.example.stayfinder.backend.event.BookingCreatedEvent;
import com.example.stayfinder.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final BookingHoldService holdService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingResponse create(BookingRequest req) {



        // Validate dates
        if (!req.getCheckOut().isAfter(req.getCheckIn())) {
            throw new RuntimeException(
                    "Check-out must be after check-in");
        }

        if (req.getCheckIn().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException(
                    "Check-in date cannot be in the past");
        }

        // Find property and guest
        Property property = propertyRepository
                .findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException(
                        "Property not found: " + req.getPropertyId()));

        User guest = userRepository
                .findById(req.getGuestId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + req.getGuestId()));

        // Check for date conflicts
        boolean overlap = bookingRepository.existsOverlappingBooking(
                req.getPropertyId(),
                req.getCheckIn(),
                req.getCheckOut()
        );

        if (overlap) {
            throw new RuntimeException(
                    "Property is already booked for these dates");
        }

        // Check Redis for active holds by other guests
        boolean held = holdService.isHeld(
                req.getPropertyId(),
                req.getCheckIn(),
                req.getCheckOut()
        );

        if (held) {
            throw new RuntimeException(
                    "These dates are temporarily held. Try again shortly");
        }
        // Calculate total price
        long nights = ChronoUnit.DAYS.between(
                req.getCheckIn(), req.getCheckOut());
        BigDecimal totalPrice = property.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights));

        // Create booking
        Booking booking = Booking.builder()
                .property(property)
                .guest(guest)
                .checkIn(req.getCheckIn())
                .checkOut(req.getCheckOut())
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .build();

        Booking saved = bookingRepository.save(booking);


        // Release hold after booking created
        holdService.releaseHold(
                req.getPropertyId(),
                req.getCheckIn(),
                req.getCheckOut()
        );
        // Save status history
        saveHistory(saved, null, BookingStatus.PENDING);

        eventPublisher.publishEvent(
                new BookingCreatedEvent(this, saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public BookingResponse confirm(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException(
                    "Only PENDING bookings can be confirmed");
        }

        BookingStatus previous = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        saveHistory(saved, previous, BookingStatus.CONFIRMED);
        eventPublisher.publishEvent(
                new BookingConfirmedEvent(this, saved));


        return toResponse(saved);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException(
                    "Booking is already cancelled");
        }

        BookingStatus previous = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        saveHistory(saved, previous, BookingStatus.CANCELLED);

        return toResponse(saved);
    }

    public BookingResponse getById(UUID id) {
        return bookingRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException(
                        "Booking not found: " + id));
    }

    public List<BookingResponse> getByProperty(UUID propertyId) {
        return bookingRepository.findByPropertyId(propertyId)
                .stream().map(this::toResponse).toList();
    }

    public List<BookingResponse> getByGuest(UUID guestId) {
        return bookingRepository.findByGuestId(guestId)
                .stream().map(this::toResponse).toList();
    }

    public boolean checkAvailability(UUID propertyId,
                                     java.time.LocalDate checkIn,
                                     java.time.LocalDate checkOut) {
        return !bookingRepository.existsOverlappingBooking(
                propertyId, checkIn, checkOut);
    }

    private void saveHistory(Booking booking,
                             BookingStatus from, BookingStatus to) {
        BookingStatusHistory history = BookingStatusHistory.builder()
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .build();
        historyRepository.save(history);
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .propertyId(b.getProperty().getId())
                .propertyTitle(b.getProperty().getTitle())
                .guestId(b.getGuest().getId())
                .guestName(b.getGuest().getName())
                .checkIn(b.getCheckIn())
                .checkOut(b.getCheckOut())
                .status(b.getStatus())
                .totalPrice(b.getTotalPrice())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
