package com.bookstore.controller;

import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.WishlistItemResponse;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.WishlistService;
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
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistItemResponse>>> get(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully.", wishlistService.getWishlist(userId(auth))));
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> add(Authentication auth, @PathVariable Long bookId) {
        wishlistService.add(userId(auth), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book added to wishlist."));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Void>> remove(Authentication auth, @PathVariable Long bookId) {
        wishlistService.remove(userId(auth), bookId);
        return ResponseEntity.ok(ApiResponse.success("Book removed from wishlist."));
    }

    private Long userId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
    }
}
