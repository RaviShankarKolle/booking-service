package com.library.notification.service;

import com.library.notification.dto.BorrowConfirmationRequest;
import com.library.notification.dto.FineReminderRequest;
import com.library.notification.dto.OverdueNotificationRequest;
import com.library.notification.dto.RegistrationNotificationRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncNotificationDispatcher {
    private final EmailNotificationService emailNotificationService;

    public AsyncNotificationDispatcher(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @Async
    public void dispatchRegistration(RegistrationNotificationRequest request) {
        emailNotificationService.sendRegistrationEmail(request);
    }

    @Async
    public void dispatchOverdue(OverdueNotificationRequest request) {
        emailNotificationService.sendOverdueEmail(request);
    }

    @Async
    public void dispatchBorrowConfirmation(BorrowConfirmationRequest request) {
        emailNotificationService.sendBorrowConfirmationEmail(request);
    }

    @Async
    public void dispatchFineReminder(FineReminderRequest request) {
        emailNotificationService.sendFineReminderEmail(request);
    }
}
