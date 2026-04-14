package com.library.fine.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "borrow-service")
public interface BorrowClient {
    @GetMapping("/api/v1/borrows/internal/overdue")
    List<OverdueBorrowRecord> getOverdueBorrows();

    record OverdueBorrowRecord(
            Long borrowId,
            Long userId,
            Long bookId,
            LocalDate dueDate,
            String status
    ) {}
}
