package com.bookstore.service.impl;

import com.bookstore.dto.response.WishlistItemResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.User;
import com.bookstore.entity.WishlistItem;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.repository.WishlistRepository;
import com.bookstore.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    public List<WishlistItemResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByIdDesc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void add(Long userId, Long bookId) {
        if (wishlistRepository.existsByUserIdAndBookId(userId, bookId)) return;
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
        wishlistRepository.save(WishlistItem.builder().user(user).book(book).build());
    }

    @Override
    @Transactional
    public void remove(Long userId, Long bookId) {
        wishlistRepository.findByUserIdAndBookId(userId, bookId).ifPresent(wishlistRepository::delete);
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Book book = item.getBook();
        return WishlistItemResponse.builder().bookId(book.getId()).title(book.getTitle()).author(book.getAuthor())
                .price(book.getPrice()).stock(book.getStock()).imageUrl(book.getImageUrl()).build();
    }
}
