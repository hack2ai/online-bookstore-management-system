package com.bookstore.service;

import com.bookstore.entity.AuditEvent;
import com.bookstore.repository.AuditEventRepository;
import com.bookstore.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock AuditEventRepository auditEventRepository;

    private final AuditServiceImpl service;

    AuditServiceImplTest() {
        service = new AuditServiceImpl(auditEventRepository);
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordsRequestIdAndAuditDetails() {
        MDC.put("X-Request-Id", "checkout-123");

        service.record("ORDER_CREATED", 7L, "ORDER", 42L, "Order created");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getUserId()).isEqualTo(7L);
        assertThat(event.getResourceType()).isEqualTo("ORDER");
        assertThat(event.getResourceId()).isEqualTo(42L);
        assertThat(event.getRequestId()).isEqualTo("checkout-123");
        assertThat(event.getDetails()).isEqualTo("Order created");
    }

    @Test
    void savesNullRequestIdWhenNoRequestContextExists() {
        service.record("SYSTEM_EVENT", null, null, null, "System event");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestId()).isNull();
    }
}
