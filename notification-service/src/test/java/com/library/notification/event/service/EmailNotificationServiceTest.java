package com.library.notification.service;

import com.library.notification.dto.BorrowConfirmationRequest;
import com.library.notification.dto.FineReminderRequest;
import com.library.notification.dto.OverdueNotificationRequest;
import com.library.notification.dto.PasswordResetNotificationRequest;
import com.library.notification.dto.RegistrationNotificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceTest {

    @Test
    void sendScheduledBatchNotice_shouldSkipWhenUsernameInvalid() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "not-an-email");

        service.sendScheduledBatchNotice();

        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendScheduledBatchNotice_shouldSendWhenConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "ops@example.com");

        service.sendScheduledBatchNotice();

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendRegistrationEmail_shouldSend() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "ops@example.com");

        service.sendRegistrationEmail(new RegistrationNotificationRequest("A", "a@example.com"));

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_shouldSwallowMailFailure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        EmailNotificationService service = new EmailNotificationService(mailSender, "ops@example.com");

        service.sendPasswordResetEmail(new PasswordResetNotificationRequest("a@example.com", "A", "token"));
    }

    @Test
    void sendOverdueEmail_shouldSend() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "ops@example.com");

        service.sendOverdueEmail(new OverdueNotificationRequest("a@example.com", "A", 1L, 2L, LocalDate.now()));

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendBorrowConfirmationEmail_shouldSend() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "ops@example.com");

        service.sendBorrowConfirmationEmail(new BorrowConfirmationRequest("a@example.com", "A", 2L, 1L, LocalDate.now().plusDays(2)));

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendFineReminderEmail_shouldSend() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "ops@example.com");

        service.sendFineReminderEmail(new FineReminderRequest("a@example.com", "A", 1L, 2L, new BigDecimal("5.00"), "Overdue"));

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
