package com.library.notification.dto;



import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;



public record PasswordResetNotificationRequest(

        @NotBlank @Email String email,

        @NotBlank String name,

        @NotBlank String resetToken

) {}


