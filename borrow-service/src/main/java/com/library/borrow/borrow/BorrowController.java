package com.library.borrow.borrow;

import com.library.borrow.common.ApiResponse;
import com.library.borrow.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.library.borrow.borrow.BorrowDtos.*;

@RestController
@RequestMapping("/api/v1/borrows")
@Tag(name = "Borrows", description = "Borrow workflow and internal overdue feed for fines.")
public class BorrowController {
    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Operation(summary = "Create borrow", description = "USER role. Reserves book and creates PENDING_PICKUP.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping
    public ResponseEntity<ApiResponse<BorrowResponse>> create(@Valid @RequestBody BorrowRequest request) {
        BorrowResponse response = borrowService.createBorrow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, Map.of("message", "Borrow created.")));
    }

    @Operation(summary = "Allocate borrow", description = "LIBRARIAN or ADMIN. Sets due date, issues book, sends confirmation email.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/{borrowId}/allocate")
    public ResponseEntity<ApiResponse<BorrowResponse>> allocate(
            @PathVariable Long borrowId,
            @Valid @RequestBody AllocateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(borrowService.allocate(borrowId, request), Map.of("message", "Borrow allocated.")));
    }

    @Operation(summary = "Return book", description = "LIBRARIAN or ADMIN. Updates book inventory and borrow status.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/{borrowId}/return")
    public ResponseEntity<ApiResponse<BorrowResponse>> returnBook(@PathVariable Long borrowId) {
        BorrowResponse response = borrowService.returnBook(borrowId);
        return ResponseEntity.ok(ApiResponse.success(response, Map.of("message", "Book returned.")));
    }

    @Operation(summary = "List borrows for user", description = "USER role. Paginated history for userId.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<BorrowListResponse>> listByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(borrowService.listByUser(userId, page, size), Map.of()));
    }

    @Operation(summary = "Overdue list (internal)", description = "For fine-payment-service. No JWT in this project.")
    @GetMapping("/internal/overdue")
    public ResponseEntity<List<OverdueBorrowResponse>> listOverdueForFine() {
        return ResponseEntity.ok(borrowService.listOverdueForFineCalculation());
    }
}
