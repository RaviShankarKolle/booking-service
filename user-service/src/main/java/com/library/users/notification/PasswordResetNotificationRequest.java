package com.library.users.notification;



public record PasswordResetNotificationRequest(

        String email,

        String name,

        String resetToken

) {}


