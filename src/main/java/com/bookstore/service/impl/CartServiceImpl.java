package com.bookstore.service.impl;

import com.bookstore.dto.request.CartItemRequest;
import com.bookstore.dto.response.CartItemResponse;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponse getCart(Long userId) {
        validateUserId(userId);
        return toResponse(cartRepository.findByUserId(userId).orElseGet(() -> emptyCart(userId)));
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        validateUserId(userId);
        validateRequest(request);

        Cart cart = getOrCreateCart(userId);
        Book book = findBook(request.getBookId());
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId()).orElse(null);
        int existingQuantity = item == null || item.getQuantity() == null ? 0 : item.getQuantity();
        int newQuantity = safeAddQuantity(existingQuantity, request.getQuantity());

        validateStock(book, newQuantity);

        if (item == null) {
            item = CartItem.builder().book(book).quantity(request.getQuantity()).build();
            cart.addItem(item);
        } else {
            item.setQuantity(newQuantity);
        }

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long bookId, int quantity) {
        validateUserId(userId);
        validateBookId(bookId);
        validateQuantity(quantity);

        Cart cart = getExistingCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item for book", bookId));
        validateStock(item.getBook(), quantity);
        item.setQuantity(quantity);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long bookId) {
        validateUserId(userId);
        validateBookId(bookId);

        Cart cart = getExistingCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item for book", bookId));
        cart.removeItem(item);
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        validateUserId(userId);
        Cart cart = getExistingCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            return cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build());
        });
    }

    private Cart getExistingCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart for user", userId));
    }

    private Cart emptyCart(Long userId) {
        return Cart.builder()
                .id(null)
                .user(User.builder().id(userId).build())
                .items(new ArrayList<>())
                .build();
    }

    private Book findBook(Long bookId) {
        validateBookId(bookId);
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
    }

    private void validateStock(Book book, int quantity) {
        int availableStock = book.getStock() == null ? 0 : Math.max(book.getStock(), 0);
        if (availableStock < quantity) {
            throw new IllegalArgumentException(
                    "Only " + availableStock + " unit(s) of '" + book.getTitle() + "' are currently available.");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                .filter(item -> item != null && item.getBook() != null)
                .map(item -> {
                    Book book = item.getBook();
                    int quantity = item.getQuantity() == null ? 0 : Math.max(item.getQuantity(), 0);
                    BigDecimal unitPrice = book.getPrice() == null ? BigDecimal.ZERO : book.getPrice();
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                    return CartItemResponse.builder()
                            .bookId(book.getId())
                            .title(book.getTitle())
                            .author(book.getAuthor())
                            .imageUrl(book.getImageUrl())
                            .unitPrice(unitPrice)
                            .quantity(quantity)
                            .subtotal(subtotal)
                            .availableStock(book.getStock())
                            .build();
                })
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = items.stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .itemCount(itemCount)
                .subtotal(subtotal)
                .build();
    }

    private void validateRequest(CartItemRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cart item request is required");
        }
        validateBookId(request.getBookId());
        if (request.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        validateQuantity(request.getQuantity());
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than zero");
        }
    }

    private void validateBookId(Long bookId) {
        if (bookId == null || bookId <= 0) {
            throw new IllegalArgumentException("Book ID must be greater than zero");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException(
                    "Quantity must be between " + MIN_QUANTITY + " and " + MAX_QUANTITY + ".");
        }
    }

    private int safeAddQuantity(int existingQuantity, int requestedQuantity) {
        if (existingQuantity < 0) {
            throw new IllegalStateException("Cart contains an invalid quantity");
        }
        try {
            int total = Math.addExact(existingQuantity, requestedQuantity);
            validateQuantity(total);
            return total;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Cart quantity is too large");
        }
    }
}
