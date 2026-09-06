package com.qify.fulfillment.web;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateQueueRequestRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validOrdinaryValuesProduceNoViolations() {
        var request = request(30, 15);

        assertTrue(validator.validate(request).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("inclusiveNumericBoundaries")
    void inclusiveNumericBoundariesProduceNoViolations(int expectedQueueMinutes, int arrivalNoticeMinutes) {
        assertTrue(validator.validate(request(expectedQueueMinutes, arrivalNoticeMinutes)).isEmpty());
    }

    static Stream<Arguments> inclusiveNumericBoundaries() {
        return Stream.of(
                Arguments.of(1, 0),
                Arguments.of(720, 120)
        );
    }

    @ParameterizedTest
    @MethodSource("requiredFieldCases")
    void eachRequiredFieldNullViolatesThatProperty(String property, CreateQueueRequestRequest request) {
        var violations = validator.validate(request);

        assertEquals(Set.of(property), violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet()));
    }

    static Stream<Arguments> requiredFieldCases() {
        return Stream.of(
                Arguments.of("serviceOfferingId", new CreateQueueRequestRequest(null, scheduledFor(), 30, 15)),
                Arguments.of("scheduledFor", new CreateQueueRequestRequest(UUID.randomUUID(), null, 30, 15)),
                Arguments.of("expectedQueueMinutes", new CreateQueueRequestRequest(UUID.randomUUID(), scheduledFor(), null, 15)),
                Arguments.of("arrivalNoticeMinutes", new CreateQueueRequestRequest(UUID.randomUUID(), scheduledFor(), 30, null))
        );
    }

    @ParameterizedTest
    @MethodSource("outOfRangeValues")
    void outOfRangeValuesViolateThatProperty(String property, CreateQueueRequestRequest request) {
        var violations = validator.validate(request);

        assertEquals(Set.of(property), violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet()));
    }

    static Stream<Arguments> outOfRangeValues() {
        return Stream.of(
                Arguments.of("expectedQueueMinutes", request(0, 15)),
                Arguments.of("expectedQueueMinutes", request(721, 15)),
                Arguments.of("arrivalNoticeMinutes", request(30, -1)),
                Arguments.of("arrivalNoticeMinutes", request(30, 121))
        );
    }

    private static CreateQueueRequestRequest request(int expectedQueueMinutes, int arrivalNoticeMinutes) {
        return new CreateQueueRequestRequest(UUID.randomUUID(), scheduledFor(), expectedQueueMinutes, arrivalNoticeMinutes);
    }

    private static OffsetDateTime scheduledFor() {
        return OffsetDateTime.parse("2026-09-07T10:00:00+08:00");
    }
}
