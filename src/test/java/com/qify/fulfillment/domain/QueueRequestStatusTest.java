package com.qify.fulfillment.domain;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class QueueRequestStatusTest {

    private static final Set<Transition> ALLOWED_TRANSITIONS = Set.of(
            new Transition(QueueRequestStatus.REQUESTED, QueueRequestStatus.ASSIGNED),
            new Transition(QueueRequestStatus.REQUESTED, QueueRequestStatus.CANCELLED),
            new Transition(QueueRequestStatus.ASSIGNED, QueueRequestStatus.ARRIVED),
            new Transition(QueueRequestStatus.ASSIGNED, QueueRequestStatus.CANCELLED),
            new Transition(QueueRequestStatus.ARRIVED, QueueRequestStatus.IN_QUEUE),
            new Transition(QueueRequestStatus.ARRIVED, QueueRequestStatus.CANCELLED),
            new Transition(QueueRequestStatus.IN_QUEUE, QueueRequestStatus.HANDOFF_READY),
            new Transition(QueueRequestStatus.IN_QUEUE, QueueRequestStatus.CANCELLED),
            new Transition(QueueRequestStatus.HANDOFF_READY, QueueRequestStatus.COMPLETED),
            new Transition(QueueRequestStatus.HANDOFF_READY, QueueRequestStatus.CANCELLED));

    @ParameterizedTest
    @MethodSource("allTransitions")
    void canTransitionToMatchesExpectedMatrix(QueueRequestStatus source, QueueRequestStatus target) {
        assertEquals(source.canTransitionTo(target), ALLOWED_TRANSITIONS.contains(new Transition(source, target)));
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void requireTransitionToAcceptsAllowedTransitions(Transition transition) {
        assertDoesNotThrow(() -> transition.source().requireTransitionTo(transition.target()));
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void requireTransitionToRejectsInvalidTransitions(Transition transition) {
        InvalidQueueRequestStatusTransitionException exception = assertThrows(
                InvalidQueueRequestStatusTransitionException.class,
                () -> transition.source().requireTransitionTo(transition.target()));
        assertTrue(exception.getMessage().contains(transition.source().toString()));
        assertTrue(exception.getMessage().contains(transition.target().toString()));
    }

    @Test
    void nullTargetIsInvalid() {
        for (QueueRequestStatus source : QueueRequestStatus.values()) {
            assertFalse(source.canTransitionTo(null));
            assertThrows(InvalidQueueRequestStatusTransitionException.class,
                    () -> source.requireTransitionTo(null));
        }
    }

    static Stream<Arguments> allTransitions() {
        return allTransitionPairs().map(transition -> Arguments.of(transition.source(), transition.target()));
    }

    static Stream<Transition> allowedTransitions() {
        return ALLOWED_TRANSITIONS.stream();
    }

    static Stream<Transition> invalidTransitions() {
        return allTransitionPairs().filter(transition -> !ALLOWED_TRANSITIONS.contains(transition));
    }

    private static Stream<Transition> allTransitionPairs() {
        return Stream.of(QueueRequestStatus.values())
                .flatMap(source -> Stream.of(QueueRequestStatus.values())
                        .map(target -> new Transition(source, target)));
    }

    record Transition(QueueRequestStatus source, QueueRequestStatus target) {
    }
}
