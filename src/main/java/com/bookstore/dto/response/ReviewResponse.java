package com.bookstore.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long bookId;
    private String customerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
