package com.bookstore.service.impl;

import com.bookstore.dto.request.BookRequest;
import com.bookstore.dto.response.BookResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Category;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<BookResponse> search(String keyword, Long categoryId, Pageable pageable) {
        String normalized = normalizeOptional(keyword);
        return bookRepository.search(normalized, categoryId, pageable).map(this::toResponse);
    }

    @Override
    public BookResponse getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Book ID must be greater than zero");
        }
        return toResponse(bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id)));
    }

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        validateRequest(request);
        String isbn = normalizeIsbn(request.getIsbn());
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateResourceException("A book with ISBN '" + isbn + "' already exists.");
        }
        Book book = new Book();
        apply(book, request, isbn);
        return toResponse(bookRepository.save(book));
    }

    @Override
    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Book ID must be greater than zero");
        }
        validateRequest(request);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        String isbn = normalizeIsbn(request.getIsbn());
        bookRepository.findByIsbn(isbn)
                .filter(existing -> existing.getId() != null && !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A book with ISBN '" + isbn + "' already exists.");
                });
        apply(book, request, isbn);
        return toResponse(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Book ID must be greater than zero");
        }
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        bookRepository.delete(book);
    }

    private void validateRequest(BookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Book request is required");
        }
        if (request.getCategoryId() == null || request.getCategoryId() <= 0) {
            throw new IllegalArgumentException("Category ID must be greater than zero");
        }
    }

    private void apply(Book book, BookRequest request, String isbn) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        book.setTitle(normalizeRequired(request.getTitle(), "Title"));
        book.setAuthor(normalizeRequired(request.getAuthor(), "Author"));
        book.setIsbn(isbn);
        book.setDescription(normalizeOptional(request.getDescription()));
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setImageUrl(normalizeOptional(request.getImageUrl()));
        book.setCategory(category);
    }

    private BookResponse toResponse(Book book) {
        if (book.getCategory() == null) {
            throw new IllegalStateException("Book '" + book.getId() + "' is not associated with a category");
        }
        int stock = book.getStock() == null ? 0 : book.getStock();
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .price(book.getPrice())
                .stock(stock)
                .inStock(stock > 0)
                .imageUrl(book.getImageUrl())
                .categoryId(book.getCategory().getId())
                .categoryName(book.getCategory().getCategoryName())
                .createdAt(book.getCreatedAt())
                .build();
    }

    private String normalizeIsbn(String value) {
        return normalizeRequired(value, "ISBN").toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }
}
