package com.library.books.book;

import com.library.books.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest {

    private BookRepository bookRepository;
    private ApplicationEventPublisher eventPublisher;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        bookService = new BookService(bookRepository, eventPublisher);
    }

    @Test
    void addBook_shouldCreateAndPublishEvent() {
        var request = new BookDtos.CreateBookRequest("Clean Code", "Robert C. Martin", "Tech", "978-0132350884", 3, "desc");
        var record = record(10L, "AVAILABLE", 3);
        when(bookRepository.findByIsbn(request.isbn())).thenReturn(Optional.empty());
        when(bookRepository.create(any(), any(), any(), any(), any(Integer.class), any())).thenReturn(10L);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(record));

        BookDtos.BookResponse response = bookService.addBook(request);

        assertEquals(10L, response.id());
        assertEquals("AVAILABLE", response.status());
        verify(eventPublisher).publishEvent(any(BookService.BookCatalogEvent.class));
    }

    @Test
    void addBook_shouldRejectDuplicateIsbn() {
        var request = new BookDtos.CreateBookRequest("Clean Code", "Robert C. Martin", "Tech", "978-0132350884", 3, "desc");
        when(bookRepository.findByIsbn(request.isbn())).thenReturn(Optional.of(record(1L, "AVAILABLE", 1)));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> bookService.addBook(request));

        assertEquals("BOOK_EXISTS", ex.getCode());
    }

    @Test
    void updateBook_shouldRejectWhenMissing() {
        var request = new BookDtos.UpdateBookRequest("T", "A", "G", "978-0132350884", 2, "d");
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class, () -> bookService.updateBook(99L, request));

        assertEquals("BOOK_NOT_FOUND", ex.getCode());
    }

    @Test
    void reserve_shouldRejectWhenNoCopyAvailable() {
        when(bookRepository.reserveCopy(2L)).thenReturn(0);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> bookService.reserve(2L));

        assertEquals("BOOK_NOT_AVAILABLE", ex.getCode());
    }

    @Test
    void issue_shouldRejectWhenMissing() {
        when(bookRepository.issueCopy(7L)).thenReturn(0);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> bookService.issue(7L));

        assertEquals("BOOK_NOT_FOUND", ex.getCode());
    }

    @Test
    void returnBook_shouldRejectWhenMissing() {
        when(bookRepository.returnCopy(7L)).thenReturn(0);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> bookService.returnBook(7L));

        assertEquals("BOOK_NOT_FOUND", ex.getCode());
    }

    @Test
    void list_shouldClampPageAndSize() {
        when(bookRepository.list(0, 100)).thenReturn(List.of(record(1L, "AVAILABLE", 2)));
        when(bookRepository.countActive()).thenReturn(1L);

        BookDtos.BookPageResponse response = bookService.list(-2, 500);

        assertEquals(0, response.page());
        assertEquals(100, response.size());
        assertEquals(1, response.items().size());
        assertEquals(1L, response.total());
        verify(bookRepository).list(0, 100);
    }

    private static BookRepository.BookRecord record(Long id, String status, int availableCopies) {
        return new BookRepository.BookRecord(
                id,
                "title",
                "author",
                "genre",
                "978-0132350884",
                3,
                availableCopies,
                status,
                "desc",
                Instant.now()
        );
    }
}
