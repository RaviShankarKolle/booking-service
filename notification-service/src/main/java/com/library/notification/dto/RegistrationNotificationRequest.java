package com.library.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationNotificationRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {}
