package com.bookstore.service;

import com.bookstore.dto.request.CartItemRequest;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock BookRepository bookRepository;
    @Mock UserRepository userRepository;

    private CartServiceImpl service;
    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        service = new CartServiceImpl(cartRepository, cartItemRepository, bookRepository, userRepository);
        user = User.builder().id(1L).name("Test").email("test@example.com").password("hash")
                .role(Role.CUSTOMER).build();
        book = Book.builder().id(10L).title("Clean Code").author("Robert C. Martin")
                .price(new BigDecimal("500.00")).stock(10).build();
    }

    @Test
    void addItemCreatesCartAndCalculatesSubtotal() {
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(20L, 10L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = service.addItem(1L, CartItemRequest.builder().bookId(10L).quantity(2).build());

        assertThat(response.getItemCount()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualByComparingTo("1000.00");
        assertThat(response.getItems()).hasSize(1);
        assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(2);
    }

    @Test
    void addExistingItemMergesQuantityInsteadOfCreatingDuplicate() {
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        CartItem existing = CartItem.builder().book(book).quantity(2).build();
        cart.getItems().add(existing);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(20L, 10L)).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addItem(1L, CartItemRequest.builder().bookId(10L).quantity(3).build());

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void addItemRejectsInsufficientStock() {
        book.setStock(2);
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(20L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(1L, CartItemRequest.builder().bookId(10L).quantity(3).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only 2 unit(s)");
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateItemRejectsQuantityAboveMaximum() {
        Cart cart = Cart.builder().id(20L).user(user).items(new ArrayList<>()).build();
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.updateItem(1L, 10L, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 100");
    }
}
