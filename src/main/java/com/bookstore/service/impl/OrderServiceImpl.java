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

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new IllegalStateException("Your cart is empty."));
        if (cart.getItems().isEmpty()) throw new IllegalStateException("Your cart is empty.");

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Order order = Order.builder().user(user).shippingAddress(request.getShippingAddress().trim())
                .status(OrderStatus.PENDING).subtotalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO).build();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Book book = bookRepository.findByIdForUpdate(cartItem.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book", cartItem.getBook().getId()));
            int quantity = cartItem.getQuantity();
            if (book.getStock() == null || book.getStock() < quantity) {
                throw new IllegalStateException("Insufficient stock for '" + book.getTitle() + "'.");
            }
            BigDecimal unitPrice = book.getPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            order.addItem(OrderItem.builder().book(book).quantity(quantity).price(unitPrice).build());
            book.setStock(book.getStock() - quantity);
        }

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            DiscountResponse discount = couponService.calculateAndReserve(userId, request.getCouponCode(), subtotal, user);
            order.setCouponCode(discount.getCode());
            order.setDiscountAmount(discount.getDiscount());
        }

        order.setSubtotalAmount(subtotal);
        order.setTotalAmount(subtotal.subtract(order.getDiscountAmount()).setScale(2));
        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);
        return toResponse(saved);
    }

    @Override
    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Override
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = find(orderId); ensureOwner(order, userId); return toResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = find(orderId); ensureOwner(order, userId);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED)
            throw new IllegalStateException("Only pending or confirmed orders can be cancelled.");
        for (OrderItem item : order.getOrderItems()) {
            Book book = bookRepository.findByIdForUpdate(item.getBook().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book", item.getBook().getId()));
            book.setStock(book.getStock() + item.getQuantity());
        }
        if (order.getCouponCode() != null && order.getStatus() == OrderStatus.PENDING) {
            couponService.releaseReservation(userId, order.getCouponCode());
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    @Override
    public Page<OrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        return (status == null ? orderRepository.findAll(pageable) : orderRepository.findByStatus(status, pageable)).map(this::toResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = find(orderId); validateTransition(order.getStatus(), status); order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    private Order find(Long id) { return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id)); }
    private void ensureOwner(Order order, Long userId) { if (!order.getUser().getId().equals(userId)) throw new ResourceNotFoundException("Order", order.getId()); }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        boolean valid = (from == OrderStatus.PENDING && (to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED))
                || (from == OrderStatus.CONFIRMED && (to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED))
                || (from == OrderStatus.SHIPPED && to == OrderStatus.DELIVERED);
        if (!valid) throw new IllegalStateException("Invalid order status transition: " + from + " -> " + to);
    }

    private OrderResponse toResponse(Order order) {
        var items = order.getOrderItems().stream().map(item -> OrderItemResponse.builder()
                .bookId(item.getBook().getId()).title(item.getBook().getTitle()).quantity(item.getQuantity())
                .unitPrice(item.getPrice()).subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).build()).toList();
        return OrderResponse.builder().id(order.getId()).subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount()).couponCode(order.getCouponCode()).totalAmount(order.getTotalAmount())
                .status(order.getStatus()).orderDate(order.getOrderDate()).shippingAddress(order.getShippingAddress()).items(items).build();
    }
}
