package com.library.fine.fine;

import com.library.fine.client.BorrowClient;
import com.library.fine.client.NotificationClient;
import com.library.fine.client.UserClient;
import com.library.fine.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FineServiceTest {

    private FineRepository fineRepository;
    private BorrowClient borrowClient;
    private NotificationClient notificationClient;
    private UserClient userClient;
    private FineService fineService;

    @BeforeEach
    void setUp() {
        fineRepository = mock(FineRepository.class);
        borrowClient = mock(BorrowClient.class);
        notificationClient = mock(NotificationClient.class);
        userClient = mock(UserClient.class);
        fineService = new FineService(fineRepository, borrowClient, notificationClient, userClient);
    }

    @Test
    void recalculateOverdueFines_returnsZeroWhenNoOverdues() {
        when(borrowClient.getOverdueBorrows()).thenReturn(List.of());

        assertEquals(0, fineService.recalculateOverdueFines());
    }

    @Test
    void recalculateOverdueFines_createsFineWhenMissing() {
        var overdue = new BorrowClient.OverdueBorrowRecord(1L, 2L, 3L, LocalDate.now().minusDays(2), "OVERDUE");
        when(borrowClient.getOverdueBorrows()).thenReturn(List.of(overdue));
        when(fineRepository.findByBorrowId(1L)).thenReturn(Optional.empty());
        when(fineRepository.create(eq(1L), eq(2L), eq(3L), any(BigDecimal.class), any(String.class), eq("PENDING"), any(LocalDate.class)))
                .thenReturn(10L);
        when(fineRepository.findById(10L)).thenReturn(Optional.of(record(10L, 1L, 2L, 3L, new BigDecimal("5.00"), "PENDING")));
        when(userClient.getProfileSummary(2L))
                .thenReturn(ApiResponse.success(new UserClient.UserNotificationContact("patron@example.com", "Patron"), Map.of()));

        assertEquals(1, fineService.recalculateOverdueFines());

        verify(fineRepository).create(eq(1L), eq(2L), eq(3L), any(BigDecimal.class), any(String.class), eq("PENDING"), any(LocalDate.class));
        verify(notificationClient).sendFineReminder(any(NotificationClient.FineReminderRequest.class));
    }

    @Test
    void recalculateOverdueFines_updatesExistingFine() {
        var overdue = new BorrowClient.OverdueBorrowRecord(1L, 2L, 3L, LocalDate.now().minusDays(1), "OVERDUE");
        when(borrowClient.getOverdueBorrows()).thenReturn(List.of(overdue));
        when(fineRepository.findByBorrowId(1L)).thenReturn(Optional.of(record(11L, 1L, 2L, 3L, new BigDecimal("2.50"), "PENDING")));
        when(fineRepository.findById(11L)).thenReturn(Optional.of(record(11L, 1L, 2L, 3L, new BigDecimal("5.00"), "PENDING")));
        when(userClient.getProfileSummary(2L))
                .thenReturn(ApiResponse.success(new UserClient.UserNotificationContact("patron@example.com", "Patron"), Map.of()));

        assertEquals(1, fineService.recalculateOverdueFines());

        verify(fineRepository).updateFine(eq(11L), any(BigDecimal.class), any(String.class), eq("PENDING"), any(LocalDate.class));
    }

    @Test
    void recalculateOverdueFines_skipsNotificationWhenEmailMissing() {
        var overdue = new BorrowClient.OverdueBorrowRecord(1L, 2L, 3L, LocalDate.now().minusDays(2), "OVERDUE");
        when(borrowClient.getOverdueBorrows()).thenReturn(List.of(overdue));
        when(fineRepository.findByBorrowId(1L)).thenReturn(Optional.empty());
        when(fineRepository.create(eq(1L), eq(2L), eq(3L), any(BigDecimal.class), any(String.class), eq("PENDING"), any(LocalDate.class)))
                .thenReturn(10L);
        when(fineRepository.findById(10L)).thenReturn(Optional.of(record(10L, 1L, 2L, 3L, new BigDecimal("5.00"), "PENDING")));
        when(userClient.getProfileSummary(2L))
                .thenReturn(ApiResponse.success(new UserClient.UserNotificationContact("", "Patron"), Map.of()));

        assertEquals(1, fineService.recalculateOverdueFines());

        verify(notificationClient, never()).sendFineReminder(any(NotificationClient.FineReminderRequest.class));
    }

    @Test
    void markLostBookFine_shouldApplyFlatFineAndMapResponse() {
        when(fineRepository.findByBorrowId(7L)).thenReturn(Optional.empty());
        when(fineRepository.create(eq(7L), eq(8L), eq(9L), eq(new BigDecimal("250.00")), eq("Lost book fine"), eq("PENDING"), any(LocalDate.class)))
                .thenReturn(77L);
        when(fineRepository.findById(77L)).thenReturn(Optional.of(record(77L, 7L, 8L, 9L, new BigDecimal("250.00"), "PENDING")));
        when(userClient.getProfileSummary(8L))
                .thenReturn(ApiResponse.success(new UserClient.UserNotificationContact("user@example.com", "User"), Map.of()));

        FineDtos.FineResponse response = fineService.markLostBookFine(7L, 8L, 9L);

        assertEquals(77L, response.id());
        assertEquals(new BigDecimal("250.00"), response.amount());
        assertEquals("PENDING", response.status());
    }

    private static FineRepository.FineRecord record(Long id, Long borrowId, Long userId, Long bookId, BigDecimal amount, String status) {
        return new FineRepository.FineRecord(id, borrowId, userId, bookId, amount, "reason", status, LocalDate.now(), Instant.now());
    }
}

