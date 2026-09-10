package com.bookstore.controller;

import com.bookstore.dto.request.CartItemRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Customer shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100;

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my cart", description = "Returns the authenticated customer's current shopping cart.")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        return noStore(ApiResponse.success("Cart retrieved successfully.",
                cartService.getCart(currentUserId(authentication))));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add a book to the cart", description = "Adds the requested book and validates quantity through the service layer.")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request) {
        return noStore(ApiResponse.success("Book added to cart.",
                cartService.addItem(currentUserId(authentication), request)));
    }

    @PutMapping("/items/{bookId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update cart quantity", description = "Updates a cart item's quantity. Quantity must be between 1 and 100.")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            Authentication authentication,
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId,
            @RequestParam int quantity) {
        if (bookId == null || bookId <= 0) {
            throw new IllegalArgumentException("Book ID must be greater than zero.");
        }
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between " + MIN_QUANTITY + " and " + MAX_QUANTITY + ".");
        }
        return noStore(ApiResponse.success("Cart updated successfully.",
                cartService.updateItem(currentUserId(authentication), bookId, quantity)));
    }

    @DeleteMapping("/items/{bookId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove a book from the cart")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            Authentication authentication,
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId) {
        cartService.removeItem(currentUserId(authentication), validatePositive(bookId, "Book ID"));
        return noStore(ApiResponse.success("Book removed from cart."));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Clear my cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        cartService.clearCart(currentUserId(authentication));
        return noStore(ApiResponse.success("Cart cleared successfully."));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)
                || details.getUser() == null || details.getUser().getId() == null) {
            throw new IllegalStateException("Authenticated user context is unavailable.");
        }
        return details.getUser().getId();
    }

    private Long validatePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
