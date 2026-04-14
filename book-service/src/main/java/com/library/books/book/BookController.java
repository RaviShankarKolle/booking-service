package com.library.books.book;

import com.library.books.common.ApiResponse;
import com.library.books.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.library.books.book.BookDtos.*;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Catalog CRUD, listing, and circulation hooks (reserve/issue/return).")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "Add book", description = "LIBRARIAN or ADMIN. ISBN must be unique.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> add(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.ok(ApiResponse.success(bookService.addBook(request), Map.of("message", "Book created.")));
    }

    @Operation(summary = "Update book", description = "LIBRARIAN or ADMIN.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookResponse>> update(
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookService.updateBook(bookId, request), Map.of("message", "Book updated.")));
    }

    @Operation(summary = "Remove book", description = "Soft delete (status REMOVED). LIBRARIAN or ADMIN.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long bookId) {
        bookService.removeBook(bookId);
        return ResponseEntity.ok(ApiResponse.success(null, Map.of("message", "Book removed.")));
    }

    @Operation(summary = "Reserve copy", description = "Called by borrow-service. No JWT in this project; protect in production.")
    @PostMapping("/{bookId}/reserve")
    public ResponseEntity<ApiResponse<BookResponse>> reserve(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.reserve(bookId), Map.of("message", "Book reserved.")));
    }

    @Operation(summary = "Issue copy", description = "Called when borrow is allocated. No JWT in this project.")
    @PostMapping("/{bookId}/issue")
    public ResponseEntity<ApiResponse<BookResponse>> issue(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.issue(bookId), Map.of("message", "Book issued.")));
    }

    @Operation(summary = "Return copy", description = "Called when borrow is returned. No JWT in this project.")
    @PostMapping("/{bookId}/return")
    public ResponseEntity<ApiResponse<BookResponse>> returnBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success(bookService.returnBook(bookId), Map.of("message", "Book returned.")));
    }

    @Operation(summary = "List available books", description = "Paginated AVAILABLE books. Authenticated user.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping
    public ResponseEntity<ApiResponse<BookPageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookService.list(page, size), Map.of()));
    }
}
