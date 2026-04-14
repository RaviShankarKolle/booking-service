package com.library.users.user;

import com.library.users.exception.ApplicationException;
import com.library.users.notification.NotificationClient;
import com.library.users.security.TokenService;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.library.users.user.UserDtos.*;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final NotificationClient notificationClient;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       NotificationClient notificationClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public void register(RegisterRequest request, String ip, String userAgent) {
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new ApplicationException("USER_EXISTS", "Email is already registered.", HttpStatus.CONFLICT);
        });
        String role = request.role() == null || request.role().isBlank() ? "USER" : request.role().toUpperCase();
        Long userId = userRepository.createUser(
                request.name(),
                request.email().toLowerCase(),
                request.phone(),
                passwordEncoder.encode(request.password())
        );
        userRepository.assignRole(userId, role);
        userRepository.auditLog(userId, "REGISTER", ip, userAgent);
        try {
            notificationClient.sendRegistrationNotification(
                    new com.library.users.notification.RegistrationNotificationRequest(
                            request.name(),
                            request.email().toLowerCase()
                    )
            );
        } catch (FeignException ex) {
            // Registration should succeed even if notification service is down.
            log.warn("Registration notification failed for {}: {}", request.email(), ex.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request, String ip, String userAgent) {
        UserRepository.UserRecord user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApplicationException("INVALID_CREDENTIALS", "Invalid credentials.", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApplicationException("INVALID_CREDENTIALS", "Invalid credentials.", HttpStatus.UNAUTHORIZED);
        }
        Set<String> roles = userRepository.getRoles(user.id());
        String token = tokenService.generateToken(user.id(), roles);
        userRepository.auditLog(user.id(), "LOGIN", ip, userAgent);
        return new AuthResponse(token, "Bearer", tokenService.getExpirationSeconds());
    }

    public void forgotPassword(ForgotPasswordRequest request, String ip, String userAgent) {
        userRepository.findByEmail(request.email().toLowerCase()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            userRepository.insertPasswordResetToken(user.id(), token, Instant.now().plus(30, ChronoUnit.MINUTES));
            userRepository.auditLog(user.id(), "FORGOT_PASSWORD", ip, userAgent);
            try {
                notificationClient.sendPasswordResetNotification(
                        new com.library.users.notification.PasswordResetNotificationRequest(
                                user.email(),
                                user.name(),
                                token
                        )
                );
            } catch (FeignException ex) {
                log.warn("Password reset notification failed for {}: {}", user.email(), ex.getMessage());
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ip, String userAgent) {
        String resetToken = request.token().trim();
        Long userId = userRepository.findValidTokenUser(resetToken)
                .orElseThrow(() -> new ApplicationException("INVALID_RESET_TOKEN", "Reset token is invalid or expired.", HttpStatus.BAD_REQUEST));
        userRepository.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
        userRepository.markTokenUsed(resetToken);
        userRepository.auditLog(userId, "RESET_PASSWORD", ip, userAgent);
    }
}
