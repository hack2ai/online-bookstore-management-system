package com.bookstore.service.impl;

import com.bookstore.dto.request.CategoryRequest;
import com.bookstore.dto.response.CategoryResponse;
import com.bookstore.entity.Category;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = normalize(request.getCategoryName());
        if (categoryRepository.existsByCategoryNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists.");
        }
        Category category = Category.builder()
                .categoryName(name)
                .description(normalizeOptional(request.getDescription()))
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = normalize(request.getCategoryName());
        categoryRepository.findByCategoryNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A category named '" + name + "' already exists.");
                });
        category.setCategoryName(name);
        category.setDescription(normalizeOptional(request.getDescription()));
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        long bookCount = categoryRepository.countBooksByCategoryId(id);
        if (bookCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category '" + category.getCategoryName() + "' because it contains " + bookCount + " book(s).");
        }
        categoryRepository.delete(category);
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .bookCount(categoryRepository.countBooksByCategoryId(category.getId()))
                .build();
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
