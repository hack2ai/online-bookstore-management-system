package com.bookstore.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WishlistItemResponse {
    private Long bookId;
    private String title;
    private String author;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
}
