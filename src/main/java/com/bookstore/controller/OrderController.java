package com.bookstore.controller;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.dto.request.OrderStatusRequest;
import com.bookstore.dto.response.ApiResponse;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.entity.OrderStatus;
import com.bookstore.security.CustomUserDetails;
import com.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse as OpenApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, customer orders and administration")
public class OrderController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Place an order", description = "Creates an order from the authenticated customer's cart and reserves requested stock.")
    @ApiResponses({
            @OpenApiResponse(responseCode = "201", description = "Order created successfully"),
            @OpenApiResponse(responseCode = "400", description = "Invalid checkout request or unavailable stock"),
            @OpenApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.placeOrder(userId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("Order placed successfully.", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List my orders", description = "Returns paginated orders belonging only to the authenticated customer.")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> myOrders(
            Authentication authentication,
            @Parameter(description = "Pagination and sorting parameters.")
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "orderDate") Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return noStore(ApiResponse.success("Orders retrieved successfully.",
                orderService.getMyOrders(userId(authentication), safePageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my order", description = "Returns a single order after enforcing ownership in the service layer.")
    public ResponseEntity<ApiResponse<OrderResponse>> myOrder(
            Authentication authentication,
            @Parameter(name = "id", in = ParameterIn.PATH, description = "Order ID", required = true)
            @PathVariable Long id) {
        return noStore(ApiResponse.success("Order retrieved successfully.",
                orderService.getMyOrder(userId(authentication), id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel my order", description = "Cancels an eligible customer order and restores reserved stock.")
    public ResponseEntity<ApiResponse<Void>> cancel(
            Authentication authentication,
            @Parameter(name = "id", in = ParameterIn.PATH, description = "Order ID", required = true)
            @PathVariable Long id) {
        orderService.cancelOrder(userId(authentication), id);
        return noStore(ApiResponse.success("Order cancelled successfully."));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all orders", description = "Administrative order listing with optional status filtering and bounded pagination.")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> allOrders(
            @Parameter(description = "Optional order status filter.")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Pagination and sorting parameters.")
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "orderDate") Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return noStore(ApiResponse.success("Orders retrieved successfully.",
                orderService.getAllOrders(status, safePageable)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status", description = "Updates an order only when the requested state transition is valid.")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @Parameter(name = "id", in = ParameterIn.PATH, description = "Order ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request) {
        return noStore(ApiResponse.success("Order status updated successfully.",
                orderService.updateStatus(id, request.getStatus())));
    }

    private Long userId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)
                || details.getUser() == null || details.getUser().getId() == null) {
            throw new IllegalStateException("Authenticated user context is unavailable.");
        }
        return details.getUser().getId();
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null || pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(ApiResponse<T> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
