package com.bookstore.repository;

import com.bookstore.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("""
            select a from AuditEvent a
            where (:eventType is null or a.eventType = :eventType)
              and (:userId is null or a.userId = :userId)
              and (:requestId is null or lower(a.requestId) like lower(concat('%', :requestId, '%')))
            order by a.createdAt desc
            """)
    Page<AuditEvent> search(
            @Param("eventType") String eventType,
            @Param("userId") Long userId,
            @Param("requestId") String requestId,
            Pageable pageable);
}
