package com.bookstore.service;

public interface AuditService {

    void record(String eventType, Long userId, String resourceType, Long resourceId, String details);
}
