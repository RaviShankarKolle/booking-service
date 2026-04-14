package com.library.borrow.borrow;

import com.library.borrow.client.BookClient;
import com.library.borrow.client.NotificationClient;
import com.library.borrow.client.UserClient;
import com.library.borrow.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.library.borrow.borrow.BorrowDtos.*;

@Service
public class BorrowService {
    private static final Logger log = LoggerFactory.getLogger(BorrowService.class);
    private static final int MAX_ACTIVE_BOOKS_PER_USER = 3;
    private static final int MAX_BORROW_DAYS = 14;

    private final BorrowRepository borrowRepository;
    private final UserClient userClient;
    private final BookClient bookClient;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;

    public BorrowService(BorrowRepository borrowRepository,
                         UserClient userClient,
                         BookClient bookClient,
                         NotificationClient notificationClient,
                         ApplicationEventPublisher eventPublisher) {
        this.borrowRepository = borrowRepository;
        this.userClient = userClient;
        this.bookClient = bookClient;
        this.notificationClient = notificationClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BorrowResponse createBorrow(BorrowRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new ApplicationException("INVALID_BORROW_WINDOW", "End date must be after start date.", HttpStatus.BAD_REQUEST);
        }
        if (request.startDate().plusDays(MAX_BORROW_DAYS).isBefore(request.endDate())) {
            throw new ApplicationException("BORROW_DURATION_EXCEEDED", "Max borrow duration is 14 days.", HttpStatus.BAD_REQUEST);
        }
        if (borrowRepository.countActiveLoans(request.userId()) >= MAX_ACTIVE_BOOKS_PER_USER) {
            throw new ApplicationException("BORROW_LIMIT_REACHED", "Max active borrow limit reached.", HttpStatus.CONFLICT);
        }
        var eligibilityResponse = userClient.getEligibility(request.userId());
        if (eligibilityResponse.data() == null || !eligibilityResponse.data().eligible()) {
            throw new ApplicationException("USER_NOT_ELIGIBLE", "User is not eligible for borrowing.", HttpStatus.CONFLICT);
        }
        bookClient.reserve(request.bookId());
        Long id = borrowRepository.createPending(request.userId(), request.bookId(), request.startDate(), request.endDate());
        BorrowResponse response = map(borrowRepository.findById(id).orElseThrow());
        publishBorrowEvent("BORROW_CREATED", response.id(), response.userId(), response.bookId(), null);
        return response;
    }

    @Transactional
    public BorrowResponse allocate(Long borrowId, AllocateRequest request) {
        BorrowRepository.BorrowRecord record = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ApplicationException("BORROW_NOT_FOUND", "Borrow record not found.", HttpStatus.NOT_FOUND));
        if (!"PENDING_PICKUP".equalsIgnoreCase(record.status())) {
            throw new ApplicationException("INVALID_BORROW_STATE", "Only pending pickup records can be allocated.", HttpStatus.CONFLICT);
        }
        LocalDate dueDate = request.dueDate();
        if (!dueDate.isAfter(LocalDate.now())) {
            throw new ApplicationException("INVALID_DUE_DATE", "Due date must be in the future.", HttpStatus.BAD_REQUEST);
        }
        bookClient.issue(record.bookId());
        borrowRepository.markAllocated(borrowId, dueDate);
        BorrowResponse response = map(borrowRepository.findById(borrowId).orElseThrow());
        publishBorrowEvent("BORROW_ALLOCATED", response.id(), response.userId(), response.bookId(), dueDate);
        return response;
    }

    @Transactional
    public BorrowResponse returnBook(Long borrowId) {
        BorrowRepository.BorrowRecord record = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ApplicationException("BORROW_NOT_FOUND", "Borrow record not found.", HttpStatus.NOT_FOUND));
        if ("RETURNED".equalsIgnoreCase(record.status())) {
            throw new ApplicationException("ALREADY_RETURNED", "Book is already returned.", HttpStatus.CONFLICT);
        }
        bookClient.returnBook(record.bookId());
        borrowRepository.markReturned(borrowId);
        BorrowResponse response = map(borrowRepository.findById(borrowId).orElseThrow());
        publishBorrowEvent("BORROW_RETURNED", response.id(), response.userId(), response.bookId(), null);
        return response;
    }

    public BorrowListResponse listByUser(Long userId, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);
        int offset = validPage * validSize;
        List<BorrowResponse> items = borrowRepository.listByUser(userId, offset, validSize).stream().map(this::map).toList();
        return new BorrowListResponse(items, validPage, validSize, borrowRepository.countByUser(userId));
    }

    @Transactional
    public int processOverdueRecords() {
        List<BorrowRepository.BorrowRecord> overdueRecords = borrowRepository.findAllocatedOverdue(LocalDate.now());
        for (BorrowRepository.BorrowRecord record : overdueRecords) {
            borrowRepository.markOverdue(record.id());
            publishBorrowEvent("BORROW_OVERDUE", record.id(), record.userId(), record.bookId(), record.dueDate());
            try {
                UserClient.UserNotificationContact profile = fetchUserProfileOrNull(record.userId());
                if (profile == null || profile.email() == null || profile.email().isBlank()) {
                    log.warn("Skipping overdue email for borrowId {}: no email for userId {}", record.id(), record.userId());
                    continue;
                }
                String displayName = profile.name() != null && !profile.name().isBlank() ? profile.name() : "Library user";
                notificationClient.sendOverdueNotification(new NotificationClient.OverdueNotificationRequest(
                        profile.email(),
                        displayName,
                        record.bookId(),
                        record.id(),
                        record.dueDate()
                ));
            } catch (Exception ex) {
                log.warn("Failed to dispatch overdue notification for borrowId {}: {}", record.id(), ex.getMessage());
            }
        }
        return overdueRecords.size();
    }

    public List<OverdueBorrowResponse> listOverdueForFineCalculation() {
        return borrowRepository.findFineCandidates(LocalDate.now()).stream()
                .map(record -> new OverdueBorrowResponse(
                        record.id(),
                        record.userId(),
                        record.bookId(),
                        record.dueDate(),
                        record.status()
                ))
                .toList();
    }

    private BorrowResponse map(BorrowRepository.BorrowRecord r) {
        return new BorrowResponse(r.id(), r.userId(), r.bookId(), r.startDate(), r.endDate(), r.dueDate(), r.status(), r.updatedAt());
    }

    /**
     * Loads email/name from user-service (backed by users DB). Returns null if user missing or response unusable.
     */
    private UserClient.UserNotificationContact fetchUserProfileOrNull(Long userId) {
        try {
            var response = userClient.getProfileSummary(userId);
            if (response == null || response.data() == null) {
                return null;
            }
            return response.data();
        } catch (Exception ex) {
            log.warn("Could not load user profile for userId {}: {}", userId, ex.getMessage());
            return null;
        }
    }

    private void publishBorrowEvent(String type, Long borrowId, Long userId, Long bookId, LocalDate dueDate) {
        eventPublisher.publishEvent(new BorrowDomainEvent(type, borrowId, userId, bookId, Instant.now()));
        log.info("Borrow event: type={}, borrowId={}, userId={}, bookId={}", type, borrowId, userId, bookId);
        if ("BORROW_ALLOCATED".equals(type) && dueDate != null) {
            try {
                UserClient.UserNotificationContact profile = fetchUserProfileOrNull(userId);
                if (profile == null || profile.email() == null || profile.email().isBlank()) {
                    log.warn("Skipping borrow confirmation for borrowId {}: no email for userId {}", borrowId, userId);
                    return;
                }
                String displayName = profile.name() != null && !profile.name().isBlank() ? profile.name() : "Library user";
                notificationClient.sendBorrowConfirmation(new NotificationClient.BorrowConfirmationRequest(
                        profile.email(),
                        displayName,
                        borrowId,
                        bookId,
                        dueDate
                ));
            } catch (Exception ex) {
                log.warn("Failed to send borrow confirmation for borrowId {}: {}", borrowId, ex.getMessage());
            }
        }
    }

    public record BorrowDomainEvent(String type, Long borrowId, Long userId, Long bookId, Instant occurredAt) {}
}
