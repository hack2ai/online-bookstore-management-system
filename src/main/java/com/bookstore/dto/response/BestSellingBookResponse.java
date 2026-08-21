package com.bookstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestSellingBookResponse {
    private Long bookId;
    private String title;
    private String author;
    private long unitsSold;
}
