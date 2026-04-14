package com.library.notification.service;

import com.library.notification.dto.PasswordResetNotificationRequest;
import com.library.notification.dto.BorrowConfirmationRequest;
import com.library.notification.dto.FineReminderRequest;
import com.library.notification.dto.OverdueNotificationRequest;
import com.library.notification.dto.RegistrationNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private final JavaMailSender mailSender;
    private final String mailUsername;

    public EmailNotificationService(JavaMailSender mailSender,
                                    @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
    }

    /** Called by {@link com.library.notification.event.NotificationBatchScheduler}; best-effort self-digest when SMTP is configured. */
    public void sendScheduledBatchNotice() {
        if (mailUsername == null || mailUsername.isBlank() || !mailUsername.contains("@")) {
            log.debug("Skipping scheduled batch mail: spring.mail.username not set or not an email.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(mailUsername);
            message.setSubject("Library Management — notification scheduler");
            message.setText("Scheduled notification batch ran at " + Instant.now()
                    + "\n\nEvent-driven emails are delivered via REST from borrow-service, fine-payment-service, and user-service.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Scheduled batch notice email skipped: {}", ex.getMessage());
        }
    }

    public void sendRegistrationEmail(RegistrationNotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.email());
            message.setSubject("Welcome to Library Management");
            message.setText("Hi " + request.name() + ",\n\nYour account has been created successfully.");
            mailSender.send(message);
        } catch (Exception ex) {
            // Keep registration flow resilient even if SMTP is not configured yet.
            log.warn("Unable to send registration email to {}: {}", request.email(), ex.getMessage());
        }
    }

    public void sendPasswordResetEmail(PasswordResetNotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.email());
            message.setSubject("Reset your Library Management password");
            message.setText("Hi " + request.name() + ",\n\n"
                    + "Use this token in the reset-password API (within 30 minutes):\n\n"
                    + request.resetToken() + "\n\n"
                    + "If you did not request this, you can ignore this email.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Unable to send password reset email to {}: {}", request.email(), ex.getMessage());
        }
    }

    public void sendOverdueEmail(OverdueNotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.email());
            message.setSubject("Overdue book alert - Library Management");
            message.setText("Hi " + request.name() + ",\n\n"
                    + "Your borrowed book is overdue.\n"
                    + "Borrow ID: " + request.borrowId() + "\n"
                    + "Book ID: " + request.bookId() + "\n"
                    + "Due Date: " + request.dueDate() + "\n\n"
                    + "Please return the book as soon as possible.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Unable to send overdue email to {}: {}", request.email(), ex.getMessage());
        }
    }

    public void sendBorrowConfirmationEmail(BorrowConfirmationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.email());
            message.setSubject("Borrow confirmation - Library Management");
            message.setText("Hi " + request.name() + ",\n\n"
                    + "Your borrow request has been allocated.\n"
                    + "Borrow ID: " + request.borrowId() + "\n"
                    + "Book ID: " + request.bookId() + "\n"
                    + "Due Date: " + request.dueDate() + "\n\n"
                    + "Please return before the due date.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Unable to send borrow confirmation email to {}: {}", request.email(), ex.getMessage());
        }
    }

    public void sendFineReminderEmail(FineReminderRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.email());
            message.setSubject("Fine reminder - Library Management");
            message.setText("Hi " + request.name() + ",\n\n"
                    + "An outstanding fine is pending payment.\n"
                    + "Fine ID: " + request.fineId() + "\n"
                    + "Amount: " + request.amount() + "\n"
                    + "Reason: " + request.reason() + "\n\n"
                    + "Please clear the fine at the earliest.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Unable to send fine reminder email to {}: {}", request.email(), ex.getMessage());
        }
    }
}
