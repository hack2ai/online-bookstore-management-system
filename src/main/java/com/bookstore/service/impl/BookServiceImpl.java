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
        String normalized = keyword == null ? null : keyword.trim();
        if (normalized != null && normalized.isEmpty()) normalized = null;
        return bookRepository.search(normalized, categoryId, pageable).map(this::toResponse);
    }

    @Override
    public BookResponse getById(Long id) {
        return toResponse(bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id)));
    }

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        String isbn = normalize(request.getIsbn());
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
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        String isbn = normalize(request.getIsbn());
        bookRepository.findByIsbn(isbn).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new DuplicateResourceException("A book with ISBN '" + isbn + "' already exists.");
        });
        apply(book, request, isbn);
        return toResponse(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
        bookRepository.delete(book);
    }

    private void apply(Book book, BookRequest request, String isbn) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(isbn);
        book.setDescription(normalizeOptional(request.getDescription()));
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setImageUrl(normalizeOptional(request.getImageUrl()));
        book.setCategory(category);
    }

    private BookResponse toResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId()).title(book.getTitle()).author(book.getAuthor()).isbn(book.getIsbn())
                .description(book.getDescription()).price(book.getPrice()).stock(book.getStock())
                .inStock(book.getStock() != null && book.getStock() > 0).imageUrl(book.getImageUrl())
                .categoryId(book.getCategory().getId()).categoryName(book.getCategory().getCategoryName())
                .createdAt(book.getCreatedAt()).build();
    }

    private String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private String normalizeOptional(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }
}
