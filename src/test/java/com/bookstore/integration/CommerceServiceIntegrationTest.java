package com.bookstore.integration;

import com.bookstore.dto.request.CartItemRequest;
import com.bookstore.dto.request.CheckoutRequest;
import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.Category;
import com.bookstore.entity.OrderStatus;
import com.bookstore.entity.PaymentStatus;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.CouponService;
import com.bookstore.service.impl.CartServiceImpl;
import com.bookstore.service.impl.OrderServiceImpl;
import com.bookstore.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommerceServiceIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired BookRepository bookRepository;
    @Autowired CartRepository cartRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired CartServiceImpl cartService;
    @Autowired OrderServiceImpl orderService;
    @Autowired PaymentServiceImpl paymentService;

    @MockBean CouponService couponService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder()
                .categoryName("Testing")
                .description("Integration tests")
                .build());

        user = userRepository.save(User.builder()
                .name("Integration User")
                .email("integration@example.com")
                .password("hashed-password")
                .role(Role.CUSTOMER)
                .build());
        book = bookRepository.save(Book.builder()
                .title("Integration Testing")
                .author("Test Author")
                .isbn("9780000000001")
                .price(new BigDecimal("499.00"))
                .stock(7)
                .category(category)
                .build());
    }

    @Test
    void cartPersistsThenCheckoutConsumesStockAndClearsCart() {
        CartResponse cart = cartService.addItem(user.getId(),
                CartItemRequest.builder().bookId(book.getId()).quantity(2).build());

        assertThat(cart.getItemCount()).isEqualTo(2);
        assertThat(cart.getSubtotal()).isEqualByComparingTo("998.00");
        assertThat(cartRepository.findByUserId(user.getId())).isPresent();

        OrderResponse order = orderService.placeOrder(user.getId(),
                CheckoutRequest.builder().shippingAddress("  123   Main Street  ").build());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getSubtotalAmount()).isEqualByComparingTo("998.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("998.00");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getShippingAddress()).isEqualTo("123 Main Street");
        assertThat(cartRepository.findByUserId(user.getId()).orElseThrow().getItems()).isEmpty();
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(orderRepository.findById(order.getId())).isPresent();
    }

    @Test
    void mockPaymentPersistsProviderOrderIdAndVerificationConfirmsOrder() {
        cartService.addItem(user.getId(),
                CartItemRequest.builder().bookId(book.getId()).quantity(1).build());
        OrderResponse order = orderService.placeOrder(user.getId(),
                CheckoutRequest.builder().shippingAddress("45 Test Road").build());

        PaymentResponse created = paymentService.createPayment(user.getId(), order.getId());

        assertThat(created.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(created.getRazorpayOrderId()).isEqualTo("mock-order-" + order.getId());
        assertThat(created.getTransactionId()).isNull();

        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId(created.getRazorpayOrderId())
                .razorpayPaymentId("ignored-in-mock-mode")
                .razorpaySignature("ignored-in-mock-mode")
                .build();
        PaymentResponse verified = paymentService.verifyPayment(user.getId(), order.getId(), request);

        assertThat(verified.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(verified.getTransactionId()).isEqualTo("mock-payment-" + order.getId());
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void checkoutWithCouponUsesReservedDiscountFromOrderTransaction() {
        cartService.addItem(user.getId(),
                CartItemRequest.builder().bookId(book.getId()).quantity(2).build());
        when(couponService.calculateAndReserve(
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq("SAVE10"),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("998.00")),
                org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(com.bookstore.dto.response.DiscountResponse.builder()
                        .code("SAVE10")
                        .discount(new BigDecimal("99.80"))
                        .originalSubtotal(new BigDecimal("998.00"))
                        .discountedSubtotal(new BigDecimal("898.20"))
                        .build());

        OrderResponse order = orderService.placeOrder(user.getId(),
                CheckoutRequest.builder().shippingAddress("45 Test Road").couponCode("SAVE10").build());

        assertThat(order.getCouponCode()).isEqualTo("SAVE10");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("99.80");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("898.20");
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getStock()).isEqualTo(5);
    }
}
