package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);

    /**
     * Single flexible query backing both "search books" and "filter by
     * category" from the spec, plus arbitrary combinations of the two,
     * rather than separate {@code findByTitle...} / {@code findByCategory...}
     * methods the service would otherwise have to pick between.
     *
     * <p>{@code keyword} is matched against title OR author (case-insensitive,
     * substring match) and is optional — pass {@code null} to skip it.
     * {@code categoryId} is also optional for the same reason. Both
     * conditions use {@code (:param IS NULL OR ...)} so a single query
     * handles "no filters", "keyword only", "category only", and "both"
     * without four near-duplicate query strings to maintain.
     */
    @Query("""
            SELECT b FROM Book b
            WHERE (:keyword IS NULL
                   OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR b.category.id = :categoryId)
            """)
    Page<Book> search(@Param("keyword") String keyword,
                       @Param("categoryId") Long categoryId,
                       Pageable pageable);

    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);
}
