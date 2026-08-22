package com.bookstore.service;

import com.bookstore.repository.AuditEventRepository;
import com.bookstore.service.impl.AdminAuditServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceImplTest {

    @Mock AuditEventRepository auditEventRepository;

    @Test
    void normalizesFiltersBeforeSearching() {
        AdminAuditServiceImpl service = new AdminAuditServiceImpl(auditEventRepository);
        Pageable pageable = PageRequest.of(0, 25);
        Page<?> result = new PageImpl<>(List.of());
        when(auditEventRepository.search("LOGIN_SUCCESS", 7L, "req-123", pageable)).thenReturn((Page) result);

        Page<?> response = service.search(" LOGIN_SUCCESS ", 7L, " req-123 ", pageable);

        assertThat(response).isSameAs(result);
        verify(auditEventRepository).search("LOGIN_SUCCESS", 7L, "req-123", pageable);
    }

    @Test
    void convertsBlankFiltersToNull() {
        AdminAuditServiceImpl service = new AdminAuditServiceImpl(auditEventRepository);
        Pageable pageable = PageRequest.of(0, 25);
        when(auditEventRepository.search(isNull(), isNull(), isNull(), pageable))
                .thenReturn(new PageImpl<>(List.of()));

        service.search("  ", null, "  ", pageable);

        verify(auditEventRepository).search(isNull(), isNull(), isNull(), pageable);
    }
}
