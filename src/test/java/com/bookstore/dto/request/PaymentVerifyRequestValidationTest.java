package com.bookstore.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentVerifyRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidGatewayIdentifiers() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .razorpaySignature("signature_123")
                .build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankGatewayIdentifiers() {
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId(" ")
                .razorpayPaymentId("")
                .razorpaySignature(null)
                .build();

        assertFalse(validator.validate(request).isEmpty());
        assertEquals(1, validator.validateProperty(request, "razorpayOrderId").size());
        assertEquals(1, validator.validateProperty(request, "razorpayPaymentId").size());
        assertEquals(1, validator.validateProperty(request, "razorpaySignature").size());
    }

    @Test
    void rejectsOversizedGatewayIdentifiers() {
        String oversized = "x".repeat(101);
        PaymentVerifyRequest request = PaymentVerifyRequest.builder()
                .razorpayOrderId(oversized)
                .razorpayPaymentId(oversized)
                .razorpaySignature(oversized)
                .build();

        assertFalse(validator.validate(request).isEmpty());
        assertEquals(1, validator.validateProperty(request, "razorpayOrderId").size());
        assertEquals(1, validator.validateProperty(request, "razorpayPaymentId").size());
        assertEquals(1, validator.validateProperty(request, "razorpaySignature").size());
    }
}
