package com.bookstore.controller;

import com.bookstore.dto.request.CartItemRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Customer shopping cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get the authenticated customer's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully.",
                cartService.getCart(currentUserId(authentication))));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a book to the cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Book added to cart.",
                cartService.addItem(currentUserId(authentication), request)));
    }

    @PutMapping("/items/{bookId}")
    @Operation(summary = "Update a cart item's quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            Authentication authentication, @PathVariable Long bookId, @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success("Cart updated successfully.",
                cartService.updateItem(currentUserId(authentication), bookId, quantity)));
    }

    @DeleteMapping("/items/{bookId}")
    @Operation(summary = "Remove a book from the cart")
    public ResponseEntity<ApiResponse<Void>> removeItem(Authentication authentication, @PathVariable Long bookId) {
        cartService.removeItem(currentUserId(authentication), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book removed from cart."));
    }

    @DeleteMapping
    @Operation(summary = "Clear the authenticated customer's cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        cartService.clearCart(currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully."));
    }

    private Long currentUserId(Authentication authentication) {
        com.bookstore.security.CustomUserDetails principal =
                (com.bookstore.security.CustomUserDetails) authentication.getPrincipal();
        return principal.getUser().getId();
    }
}
