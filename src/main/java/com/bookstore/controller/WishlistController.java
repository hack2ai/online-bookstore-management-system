package com.bookstore.controller;

import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.WishlistItemResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Wishlist", description = "Customer wishlist management")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> get(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully.", wishlistService.getWishlist(userId(auth))));
    }

    @PostMapping("/{bookId}")
    @Operation(summary = "Add a book to my wishlist")
    public ResponseEntity<ApiResponse<Void>> add(
            Authentication auth,
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId) {
        wishlistService.add(userId(auth), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book added to wishlist."));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Remove a book from my wishlist")
    public ResponseEntity<ApiResponse<Void>> remove(
            Authentication auth,
            @Parameter(name = "bookId", in = ParameterIn.PATH, description = "Book ID", required = true)
            @PathVariable Long bookId) {
        wishlistService.remove(userId(auth), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book removed from wishlist."));
    }

    private Long userId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
    }
}
