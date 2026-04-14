package com.library.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OverdueNotificationRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotNull Long bookId,
        @NotNull Long borrowId,
        @NotNull LocalDate dueDate
) {}
