package com.bookstore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {
    @NotBlank @Size(max = 255) private String title;
    @NotBlank @Size(max = 150) private String author;
    @NotBlank @Size(max = 20) private String isbn;
    @Size(max = 2000) private String description;
    @NotNull @DecimalMin(value = "0.00") private BigDecimal price;
    @NotNull @Min(0) private Integer stock;
    @Size(max = 500) private String imageUrl;
    @NotNull private Long categoryId;
}