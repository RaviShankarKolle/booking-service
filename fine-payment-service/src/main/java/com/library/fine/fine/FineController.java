package com.library.fine.fine;

import com.library.fine.common.ApiResponse;
import com.library.fine.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fines")
@Tag(name = "Fines", description = "Overdue recalculation and lost-book penalties.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @Operation(summary = "Recalculate overdue fines",
            description = "Fetches overdue borrows from borrow-service, upserts fine rows, sends reminders via user + notification services.")
    @PostMapping("/recalculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalculate() {
        int updated = fineService.recalculateOverdueFines();
        return ResponseEntity.ok(ApiResponse.success(Map.of("updatedFines", updated), Map.of("message", "Fine recalculation complete.")));
    }

    @Operation(summary = "Lost book fine",
            description = "Applies flat lost-book fine for borrowId; upserts fines table and notifies patron.")
    @PostMapping("/lost/{borrowId}/users/{userId}/books/{bookId}")
    public ResponseEntity<ApiResponse<FineDtos.FineResponse>> markLost(
            @PathVariable Long borrowId,
            @PathVariable Long userId,
            @PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(
                fineService.markLostBookFine(borrowId, userId, bookId),
                Map.of("message", "Lost-book fine applied.")
        ));
    }
}
