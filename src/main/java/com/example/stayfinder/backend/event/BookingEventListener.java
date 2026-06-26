package com.example.stayfinder.backend.event;



import com.example.stayfinder.backend.entity.Booking;
import com.example.stayfinder.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleBookingCreated(BookingCreatedEvent event) {
        Booking booking = event.getBooking();
        log.info("Booking created event received for: {}",
                booking.getId());

        emailService.sendBookingCreatedEmail(
                booking.getGuest().getEmail(),
                booking.getGuest().getName(),
                booking.getProperty().getTitle(),
                booking.getCheckIn().toString(),
                booking.getCheckOut().toString(),
                booking.getTotalPrice().toString()
        );
    }

    @Async
    @EventListener
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        Booking booking = event.getBooking();
        log.info("Booking confirmed event received for: {}",
                booking.getId());

        emailService.sendBookingConfirmedEmail(
                booking.getGuest().getEmail(),
                booking.getGuest().getName(),
                booking.getProperty().getTitle(),
                booking.getCheckIn().toString(),
                booking.getCheckOut().toString(),
                booking.getTotalPrice().toString(),
                booking.getId().toString()
        );
    }
}