package com.library.notification.controller;

import com.library.notification.dto.PasswordResetNotificationRequest;
import com.library.notification.dto.BorrowConfirmationRequest;
import com.library.notification.dto.FineReminderRequest;
import com.library.notification.dto.OverdueNotificationRequest;
import com.library.notification.dto.RegistrationNotificationRequest;
import com.library.notification.service.AsyncNotificationDispatcher;
import com.library.notification.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Email dispatch endpoints invoked by other microservices.")
public class NotificationController {

    private final EmailNotificationService emailNotificationService;
    private final AsyncNotificationDispatcher asyncNotificationDispatcher;

    public NotificationController(EmailNotificationService emailNotificationService,
                                  AsyncNotificationDispatcher asyncNotificationDispatcher) {
        this.emailNotificationService = emailNotificationService;
        this.asyncNotificationDispatcher = asyncNotificationDispatcher;
    }

    @Operation(summary = "Registration email", description = "Async dispatch; returns 202 Accepted.")
    @PostMapping("/registration")
    public ResponseEntity<Void> sendRegistrationNotification(@Valid @RequestBody RegistrationNotificationRequest request) {
        asyncNotificationDispatcher.dispatchRegistration(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Password reset email", description = "Synchronous send in current implementation.")
    @PostMapping("/password-reset")
    public ResponseEntity<Void> sendPasswordResetNotification(@Valid @RequestBody PasswordResetNotificationRequest request) {
        emailNotificationService.sendPasswordResetEmail(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Overdue email", description = "Called by borrow-service scheduler / flow.")
    @PostMapping("/overdue")
    public ResponseEntity<Void> sendOverdueNotification(@Valid @RequestBody OverdueNotificationRequest request) {
        asyncNotificationDispatcher.dispatchOverdue(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Borrow confirmation email", description = "Called when borrow is allocated.")
    @PostMapping("/borrow-confirmation")
    public ResponseEntity<Void> sendBorrowConfirmation(@Valid @RequestBody BorrowConfirmationRequest request) {
        asyncNotificationDispatcher.dispatchBorrowConfirmation(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Fine reminder email", description = "Called after fine upsert in fine-payment-service.")
    @PostMapping("/fine-reminder")
    public ResponseEntity<Void> sendFineReminder(@Valid @RequestBody FineReminderRequest request) {
        asyncNotificationDispatcher.dispatchFineReminder(request);
        return ResponseEntity.accepted().build();
    }
}
