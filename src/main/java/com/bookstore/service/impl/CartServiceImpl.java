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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponse getCart(Long userId) {
        return toResponse(cartRepository.findByUserId(userId).orElseGet(() -> emptyCart(userId)));
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Book book = findBook(request.getBookId());
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId()).orElse(null);

        int newQuantity = request.getQuantity() + (item == null ? 0 : item.getQuantity());
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
        if (quantity < 1 || quantity > 100) {
            throw new IllegalArgumentException("Quantity must be between 1 and 100.");
        }
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
        Cart cart = getExistingCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item for book", bookId));
        cart.removeItem(item);
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
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
        return Cart.builder().id(null).user(User.builder().id(userId).build()).items(new ArrayList<>()).build();
    }

    private Book findBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
    }

    private void validateStock(Book book, int quantity) {
        if (book.getStock() == null || book.getStock() < quantity) {
            throw new IllegalArgumentException(
                    "Only " + Math.max(book.getStock() == null ? 0 : book.getStock(), 0)
                            + " unit(s) of '" + book.getTitle() + "' are currently available.");
        }
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream().map(item -> {
            Book book = item.getBook();
            BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartItemResponse.builder()
                    .bookId(book.getId()).title(book.getTitle()).author(book.getAuthor())
                    .imageUrl(book.getImageUrl()).unitPrice(book.getPrice())
                    .quantity(item.getQuantity()).subtotal(subtotal).availableStock(book.getStock()).build();
        }).toList();
        BigDecimal subtotal = items.stream().map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        return CartResponse.builder().cartId(cart.getId()).items(items).itemCount(itemCount).subtotal(subtotal).build();
    }
}
