package com.bookstore.service;

import com.bookstore.dto.response.WishlistItemResponse;
import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getWishlist(Long userId);
    void add(Long userId, Long bookId);
    void remove(Long userId, Long bookId);
}
