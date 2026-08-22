package com.bookstore.service.impl;

import com.bookstore.entity.AuditEvent;
import com.bookstore.repository.AuditEventRepository;
import com.bookstore.service.AdminAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

    private final AuditEventRepository auditEventRepository;

    @Override
    public Page<AuditEvent> search(String eventType, Long userId, String requestId, Pageable pageable) {
        String normalizedEventType = StringUtils.hasText(eventType) ? eventType.trim() : null;
        String normalizedRequestId = StringUtils.hasText(requestId) ? requestId.trim() : null;
        return auditEventRepository.search(normalizedEventType, userId, normalizedRequestId, pageable);
    }
}
