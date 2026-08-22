package com.bookstore.service;

import com.bookstore.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditService {
    Page<AuditEvent> search(String eventType, Long userId, String requestId, Pageable pageable);
}
