package com.bookstore.service;

import com.bookstore.dto.request.CategoryRequest;
import com.bookstore.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    CategoryResponse getById(Long id);
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
}
