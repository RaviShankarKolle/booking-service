package com.library.users.user;

import com.library.users.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.library.users.user.UserDtos.UserEligibilityResponse;
import static com.library.users.user.UserDtos.UserNotificationContactResponse;

@RestController
@RequestMapping("/api/v1/internal/users")
@Tag(name = "Internal (peer services)", description = "Service-to-service APIs. No JWT; secure at network / gateway in production.")
public class InternalUserController {

    private final UserProfileService userProfileService;

    public InternalUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Operation(summary = "Borrow eligibility", description = "Used by borrow-service before creating a borrow.")
    @GetMapping("/{userId}/eligibility")
    public ResponseEntity<ApiResponse<UserEligibilityResponse>> getEligibility(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getUserEligibility(userId), Map.of()));
    }

    @Operation(summary = "Notification contact", description = "Email and name for emails (borrow confirmation, overdue, fine reminder).")
    @GetMapping("/{userId}/profile-summary")
    public ResponseEntity<ApiResponse<UserNotificationContactResponse>> getProfileSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getNotificationContact(userId), Map.of()));
    }
}
