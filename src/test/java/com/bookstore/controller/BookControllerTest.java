package com.bookstore.controller;

import com.bookstore.service.BookService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class BookControllerTest {

    private final BookController controller = new BookController(mock(BookService.class));

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> controller.search(null, null, -1, 12, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be >= 0");
    }

    @Test
    void acceptsPageSizeWithinAllowedRange() {
        assertThatCode(() -> controller.search(null, null, 0, 12, "createdAt", "desc"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPageSizeOutsideAllowedRange() {
        assertThatThrownBy(() -> controller.search(null, null, 0, 101, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @Test
    void rejectsZeroPageSize() {
        assertThatThrownBy(() -> controller.search(null, null, 0, 0, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be between 1 and 100");
    }

    @Test
    void rejectsNonPositiveCategoryId() {
        assertThatThrownBy(() -> controller.search(null, 0L, 0, 12, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("categoryId must be greater than zero");
    }

    @Test
    void rejectsOverlongKeyword() {
        String keyword = "a".repeat(101);

        assertThatThrownBy(() -> controller.search(keyword, null, 0, 12, "createdAt", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("keyword must be at most 100 characters");
    }

    @Test
    void rejectsUnsupportedSortProperty() {
        assertThatThrownBy(() -> controller.search(null, null, 0, 12, "isbn", "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported sortBy. Allowed values: title, author, price, stock, createdAt");
    }

    @Test
    void rejectsUnsupportedSortDirection() {
        assertThatThrownBy(() -> controller.search(null, null, 0, 12, "createdAt", "sideways"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("direction must be either asc or desc");
    }

    @Test
    void rejectsInvalidBookId() {
        assertThatThrownBy(() -> controller.getById(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book ID must be greater than zero");
    }

    @Test
    void updateAndDeleteRejectInvalidBookId() {
        assertThatThrownBy(() -> controller.update(-1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book ID must be greater than zero");

        assertThatThrownBy(() -> controller.delete(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book ID must be greater than zero");
    }
}
