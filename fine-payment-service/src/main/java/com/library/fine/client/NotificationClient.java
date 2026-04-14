package com.library.fine.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "notification-service")
public interface NotificationClient {
    @PostMapping("/api/v1/notifications/fine-reminder")
    void sendFineReminder(@RequestBody FineReminderRequest request);

    record FineReminderRequest(
            String email,
            String name,
            Long userId,
            Long fineId,
            BigDecimal amount,
            String reason
    ) {}
}
