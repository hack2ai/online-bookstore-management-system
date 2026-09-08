package com.bookstore.service.impl;

import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.dto.response.DiscountResponse;
import com.bookstore.dto.response.OrderItemResponse;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.User;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CouponService;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, CheckoutRequest request) {
        validateUserId(userId);
        validateCheckoutRequest(request);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Your cart is empty."));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Order order = Order.builder()
                .user(user)
                .shippingAddress(normalizeAddress(request.getShippingAddress()))
                .status(OrderStatus.PENDING)
                .subtotalAmount(ZERO)
                .discountAmount(ZERO)
                .totalAmount(ZERO)
                .build();

        BigDecimal subtotal = ZERO;
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem == null || cartItem.getBook() == null || cartItem.getQuantity() == null) {
                throw new IllegalStateException("Your cart contains an invalid item.");
            }
            if (cartItem.getQuantity() <= 0) {
                throw new IllegalStateException("Cart quantity must be greater than zero.");
            }

            Long bookId = cartItem.getBook().getId();
            Book book = bookRepository.findByIdForUpdate(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book", bookId));
            int quantity = cartItem.getQuantity();
            int availableStock = book.getStock() == null ? 0 : book.getStock();

            if (availableStock < quantity) {
                throw new IllegalStateException("Insufficient stock for '" + book.getTitle() + "'.");
            }
            if (book.getPrice() == null || book.getPrice().signum() < 0) {
                throw new IllegalStateException("Book '" + book.getTitle() + "' has an invalid price.");
            }

            BigDecimal unitPrice = book.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            order.addItem(OrderItem.builder().book(book).quantity(quantity).price(unitPrice).build());
            book.setStock(availableStock - quantity);
        }

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            DiscountResponse discount = couponService.calculateAndReserve(
                    userId, request.getCouponCode().trim(), subtotal, user);
            if (discount != null) {
                order.setCouponCode(discount.getCode());
                order.setDiscountAmount(nonNegative(discount.getDiscount()));
            }
        }

        BigDecimal discountAmount = nonNegative(order.getDiscountAmount());
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
            order.setDiscountAmount(discountAmount);
        }

        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal.subtract(discountAmount).setScale(2));

        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);
        return toResponse(saved);
    }

    @Override
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        validateUserId(userId);
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Override
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        validateUserId(userId);
        validateOrderId(orderId);
        Order order = find(orderId);
        ensureOwner(order, userId);
        return toResponse(order);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        validateOrderId(orderId);
        return toResponse(find(orderId));
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        validateUserId(userId);
        validateOrderId(orderId);

        Order order = find(orderId);
        ensureOwner(order, userId);
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed orders can be cancelled.");
        }

        for (OrderItem item : order.getOrderItems()) {
            if (item == null || item.getBook() == null || item.getBook().getId() == null) {
                throw new IllegalStateException("Order contains an invalid item.");
            }
            Book book = bookRepository.findByIdForUpdate(item.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book", item.getBook().getId()));
            int stock = book.getStock() == null ? 0 : book.getStock();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0) {
                throw new IllegalStateException("Order item quantity must be greater than zero.");
            }
            book.setStock(stock + quantity);
        }

        if (order.getCouponCode() != null && currentStatus == OrderStatus.PENDING) {
            couponService.releaseReservation(userId, order.getCouponCode());
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    @Override
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        return (status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        validateOrderId(orderId);
        if (status == null) {
            throw new IllegalArgumentException("Order status is required");
        }

        Order order = find(orderId);
        validateTransition(order.getStatus(), status);
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    private Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private void ensureOwner(Order order, Long userId) {
        if (order.getUser() == null || order.getUser().getId() == null
                || !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order", order.getId());
        }
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        boolean valid = from != null && ((from == OrderStatus.PENDING
                && (to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED))
                || (from == OrderStatus.CONFIRMED
                && (to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED))
                || (from == OrderStatus.SHIPPED && to == OrderStatus.DELIVERED));

        if (!valid) {
            throw new IllegalStateException("Invalid order status transition: " + from + " -> " + to);
        }
    }

    private OrderResponse toResponse(Order order) {
        var items = order.getOrderItems() == null ? java.util.List.<OrderItemResponse>of()
                : order.getOrderItems().stream()
                .filter(item -> item != null && item.getBook() != null)
                .map(item -> {
                    BigDecimal price = item.getPrice() == null ? ZERO : item.getPrice();
                    int quantity = item.getQuantity() == null ? 0 : Math.max(item.getQuantity(), 0);
                    return OrderItemResponse.builder()
                            .bookId(item.getBook().getId())
                            .title(item.getBook().getTitle())
                            .quantity(quantity)
                            .unitPrice(price)
                            .subtotal(price.multiply(BigDecimal.valueOf(quantity)))
                            .build();
                })
                .toList();
        var paymentStatus = order.getPayment() == null ? null : order.getPayment().getPaymentStatus();

        return OrderResponse.builder()
                .id(order.getId())
                .subtotalAmount(nonNegative(order.getSubtotalAmount()))
                .discountAmount(nonNegative(order.getDiscountAmount()))
                .couponCode(order.getCouponCode())
                .totalAmount(nonNegative(order.getTotalAmount()))
                .status(order.getStatus())
                .paymentStatus(paymentStatus)
                .orderDate(order.getOrderDate())
                .shippingAddress(order.getShippingAddress())
                .items(items)
                .build();
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be greater than zero");
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be greater than zero");
        }
    }

    private void validateCheckoutRequest(CheckoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Checkout request is required");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Shipping address is required");
        }
    }

    private String normalizeAddress(String address) {
        return address.trim().replaceAll("\\s+", " ");
    }

    private BigDecimal nonNegative(BigDecimal amount) {
        return amount == null || amount.signum() < 0 ? ZERO : amount;
    }
}
