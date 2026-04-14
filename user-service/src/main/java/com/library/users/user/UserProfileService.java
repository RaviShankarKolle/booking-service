package com.library.users.user;

import com.library.users.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.library.users.user.UserDtos.UserEligibilityResponse;
import static com.library.users.user.UserDtos.UserNotificationContactResponse;
import static com.library.users.user.UserDtos.UserProfileResponse;
import static com.library.users.user.UserDtos.UpdateProfileRequest;

@Service
public class UserProfileService {
    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getCurrentUserProfile(Long userId) {
        UserRepository.UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("USER_NOT_FOUND", "User not found.", HttpStatus.NOT_FOUND));
        Set<String> roles = userRepository.getRoles(user.id());
        return new UserProfileResponse(user.id(), user.name(), user.email(), user.phone(), user.status(), roles);
    }

    @Transactional
    public UserProfileResponse updateCurrentUserProfile(Long userId, UpdateProfileRequest request, String ip, String userAgent) {
        userRepository.updateProfile(userId, request.name(), request.phone());
        userRepository.auditLog(userId, "UPDATE_PROFILE", ip, userAgent);
        return getCurrentUserProfile(userId);
    }

    public UserEligibilityResponse getUserEligibility(Long userId) {
        UserRepository.UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("USER_NOT_FOUND", "User not found.", HttpStatus.NOT_FOUND));
        boolean eligible = "ACTIVE".equalsIgnoreCase(user.status());
        String reason = eligible ? "USER_ACTIVE" : "USER_NOT_ACTIVE";
        return new UserEligibilityResponse(user.id(), eligible, reason);
    }

    /** Email/name only for internal callers (e.g. borrow notifications); does not query {@code user_roles}. */
    public UserNotificationContactResponse getNotificationContact(Long userId) {
        UserRepository.UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException("USER_NOT_FOUND", "User not found.", HttpStatus.NOT_FOUND));
        return new UserNotificationContactResponse(user.email(), user.name());
    }
}
