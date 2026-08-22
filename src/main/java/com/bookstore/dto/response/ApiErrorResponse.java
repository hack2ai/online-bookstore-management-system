package com.bookstore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stable API contract for error responses.
 *
 * The requestId is also returned in the X-Request-Id response header so a
 * client can correlate an error with the server-side request trace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String error;
    private String message;
    private String path;
    private String requestId;

    public static ApiErrorResponse of(int status, String error, String message, String path, String requestId) {
        return ApiErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .requestId(requestId)
                .build();
    }
}
