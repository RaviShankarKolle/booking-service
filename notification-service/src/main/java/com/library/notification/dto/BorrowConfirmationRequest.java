package com.library.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BorrowConfirmationRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotNull Long borrowId,
        @NotNull Long bookId,
        @NotNull LocalDate dueDate
) {}
