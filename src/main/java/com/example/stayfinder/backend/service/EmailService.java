package com.example.stayfinder.backend.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendEmail(String to, String subject,
                          String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(
                    templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}",
                    to, e.getMessage());
        }
    }

    @Async
    public void sendBookingCreatedEmail(String guestEmail,
                                        String guestName, String propertyTitle,
                                        String checkIn, String checkOut,
                                        String totalPrice) {
        Map<String, Object> vars = Map.of(
                "guestName", guestName,
                "propertyTitle", propertyTitle,
                "checkIn", checkIn,
                "checkOut", checkOut,
                "totalPrice", totalPrice
        );
        sendEmail(guestEmail,
                "Booking Request Received — StayFinder",
                "booking-created", vars);
    }

    @Async
    public void sendBookingConfirmedEmail(String guestEmail,
                                          String guestName, String propertyTitle,
                                          String checkIn, String checkOut,
                                          String totalPrice, String bookingId) {
        Map<String, Object> vars = Map.of(
                "guestName", guestName,
                "propertyTitle", propertyTitle,
                "checkIn", checkIn,
                "checkOut", checkOut,
                "totalPrice", totalPrice,
                "bookingId", bookingId
        );
        sendEmail(guestEmail,
                "Booking Confirmed — StayFinder",
                "booking-confirmed", vars);
    }
}