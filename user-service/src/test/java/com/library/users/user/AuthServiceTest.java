package com.library.users.user;

import com.library.users.exception.ApplicationException;
import com.library.users.notification.NotificationClient;
import com.library.users.security.TokenService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private NotificationClient notificationClient;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        notificationClient = mock(NotificationClient.class);
        authService = new AuthService(userRepository, passwordEncoder, tokenService, notificationClient);
    }

    @Test
    void register_shouldRejectExistingEmail() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user(1L)));

        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                authService.register(new UserDtos.RegisterRequest("Alice", "alice@example.com", "password123", "9999999999", "USER"),
                        "127.0.0.1", "JUnit"));

        assertEquals("USER_EXISTS", ex.getCode());
    }

    @Test
    void register_shouldCreateAssignRoleAndNotify() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.createUser("Alice", "alice@example.com", "9999999999", "encoded")).thenReturn(1L);

        authService.register(new UserDtos.RegisterRequest("Alice", "alice@example.com", "password123", "9999999999", null),
                "127.0.0.1", "JUnit");

        verify(userRepository).assignRole(1L, "USER");
        verify(userRepository).auditLog(1L, "REGISTER", "127.0.0.1", "JUnit");
        verify(notificationClient).sendRegistrationNotification(any());
    }

    @Test
    void register_shouldSwallowNotificationFailure() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.createUser(anyString(), anyString(), anyString(), anyString())).thenReturn(1L);
        doThrow(mock(FeignException.class)).when(notificationClient).sendRegistrationNotification(any());

        authService.register(new UserDtos.RegisterRequest("Alice", "alice@example.com", "password123", "9999999999", "ADMIN"),
                "127.0.0.1", "JUnit");
    }

    @Test
    void login_shouldRejectUnknownUser() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                authService.login(new UserDtos.LoginRequest("unknown@example.com", "secret"), "127.0.0.1", "JUnit"));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    @Test
    void login_shouldRejectBadPassword() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user(1L)));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                authService.login(new UserDtos.LoginRequest("alice@example.com", "wrong"), "127.0.0.1", "JUnit"));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    @Test
    void login_shouldReturnTokenAndAudit() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user(1L)));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(userRepository.getRoles(1L)).thenReturn(Set.of("USER"));
        when(tokenService.generateToken(1L, Set.of("USER"))).thenReturn("jwt-token");
        when(tokenService.getExpirationSeconds()).thenReturn(7200L);

        UserDtos.AuthResponse response = authService.login(
                new UserDtos.LoginRequest("alice@example.com", "password123"), "127.0.0.1", "JUnit");

        assertEquals("jwt-token", response.accessToken());
        assertEquals(7200L, response.expiresInSeconds());
        verify(userRepository).auditLog(1L, "LOGIN", "127.0.0.1", "JUnit");
    }

    @Test
    void forgotPassword_shouldNoopWhenUserMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new UserDtos.ForgotPasswordRequest("missing@example.com"), "127.0.0.1", "JUnit");

        verify(userRepository, never()).insertPasswordResetToken(any(), any(), any());
    }

    @Test
    void forgotPassword_shouldStoreTokenAndNotifyWhenUserExists() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user(1L)));

        authService.forgotPassword(new UserDtos.ForgotPasswordRequest("alice@example.com"), "127.0.0.1", "JUnit");

        verify(userRepository).insertPasswordResetToken(eq(1L), anyString(), any());
        verify(userRepository).auditLog(1L, "FORGOT_PASSWORD", "127.0.0.1", "JUnit");
        verify(notificationClient).sendPasswordResetNotification(any());
    }

    @Test
    void resetPassword_shouldRejectInvalidToken() {
        when(userRepository.findValidTokenUser("bad-token")).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                authService.resetPassword(new UserDtos.ResetPasswordRequest("  bad-token  ", "newPassword123"),
                        "127.0.0.1", "JUnit"));

        assertEquals("INVALID_RESET_TOKEN", ex.getCode());
    }

    @Test
    void resetPassword_shouldTrimTokenUpdateAndAudit() {
        when(userRepository.findValidTokenUser("token-1")).thenReturn(Optional.of(1L));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-password");

        authService.resetPassword(new UserDtos.ResetPasswordRequest("  token-1  ", "newPassword123"),
                "127.0.0.1", "JUnit");

        verify(userRepository).updatePassword(1L, "encoded-password");
        verify(userRepository).markTokenUsed("token-1");
        verify(userRepository).auditLog(1L, "RESET_PASSWORD", "127.0.0.1", "JUnit");
    }

    private static UserRepository.UserRecord user(Long id) {
        return new UserRepository.UserRecord(id, "Alice", "alice@example.com", "9999999999", "hash", "ACTIVE");
    }
}
