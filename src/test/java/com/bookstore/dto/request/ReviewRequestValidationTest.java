package com.bookstore.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidReview() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Great book and very useful.");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsReviewWithoutOptionalComment() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(1);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRating() {
        ReviewRequest request = new ReviewRequest();
        request.setComment("No rating provided");

        assertThat(validator.validateProperty(request, "rating")).hasSize(1);
    }

    @Test
    void rejectsRatingOutsideAllowedRange() {
        ReviewRequest tooLow = new ReviewRequest();
        tooLow.setRating(0);

        ReviewRequest tooHigh = new ReviewRequest();
        tooHigh.setRating(6);

        assertThat(validator.validateProperty(tooLow, "rating")).hasSize(1);
        assertThat(validator.validateProperty(tooHigh, "rating")).hasSize(1);
    }

    @Test
    void rejectsOversizedComment() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(4);
        request.setComment("x".repeat(2001));

        assertThat(validator.validateProperty(request, "comment")).hasSize(1);
    }
}
