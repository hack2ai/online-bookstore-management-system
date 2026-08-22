package com.bookstore.service.impl;

import com.bookstore.entity.AuditEvent;
import com.bookstore.repository.AuditEventRepository;
import com.bookstore.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final String REQUEST_ID_KEY = "X-Request-Id";

    private final AuditEventRepository auditEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String eventType, Long userId, String resourceType, Long resourceId, String details) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .requestId(normalize(MDC.get(REQUEST_ID_KEY)))
                .details(normalize(details))
                .build();

        auditEventRepository.save(event);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
