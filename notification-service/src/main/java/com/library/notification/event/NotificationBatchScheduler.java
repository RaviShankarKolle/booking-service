package com.library.notification.event;

import com.library.notification.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic batch hook for library notifications. Domain emails (registration, borrow, overdue, fines)
 * are sent via {@link com.library.notification.controller.NotificationController} and Spring Mail;
 * Kafka consumers have been removed in favor of Feign calls from peer services plus this scheduler.
 */
@Component
public class NotificationBatchScheduler {
    private static final Logger log = LoggerFactory.getLogger(NotificationBatchScheduler.class);

    private final EmailNotificationService emailNotificationService;

    public NotificationBatchScheduler(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    /** Every 30 minutes: optional operational digest (no-op if SMTP not usable). */
    @Scheduled(cron = "0 0/30 * * * *")
    public void runReminderBatch() {
        log.info("Notification batch scheduler tick.");
        emailNotificationService.sendScheduledBatchNotice();
    }
}
