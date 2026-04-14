package com.library.users.notification;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/v1/notifications/registration")
    void sendRegistrationNotification(@RequestBody RegistrationNotificationRequest request);

    @PostMapping("/api/v1/notifications/password-reset")
    void sendPasswordResetNotification(@RequestBody PasswordResetNotificationRequest request);
}
