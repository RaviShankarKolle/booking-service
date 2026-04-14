package com.library.notification.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FineReminderRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotNull Long userId,
        @NotNull Long fineId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String reason
) {}
