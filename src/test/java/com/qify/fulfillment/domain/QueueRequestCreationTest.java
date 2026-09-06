package com.qify.fulfillment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import com.qify.catalog.domain.ServiceOffering;
import com.qify.identity.domain.Actor;
import com.qify.identity.domain.ActorRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QueueRequestCreationTest {

    private static final UUID REQUEST_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-06T02:00:00Z");
    private static final Instant SCHEDULED_FOR = Instant.parse("2026-09-06T03:00:00Z");

    @Test
    void createsRequestedRequestAndInitialTimelineEntry() {
        Actor customer = actor(ActorRole.CUSTOMER);
        ServiceOffering offering = offering(true, true);

        QueueRequest request = QueueRequest.create(REQUEST_ID, customer, offering, SCHEDULED_FOR, 45, 15, NOW);
        RequestTimelineEntry timeline = RequestTimelineEntry.initialFor(request, NOW);

        assertEquals(REQUEST_ID, request.getId());
        assertEquals(customer, request.getCustomer());
        assertEquals(offering, request.getServiceOffering());
        assertEquals(QueueRequestStatus.REQUESTED, request.getStatus());
        assertEquals(SCHEDULED_FOR, request.getScheduledFor());
        assertEquals(45, request.getExpectedQueueMinutes());
        assertEquals(15, request.getArrivalNoticeMinutes());
        assertEquals(NOW, request.getCreatedAt());
        assertEquals(request, timeline.getRequest());
        assertEquals(QueueRequestStatus.REQUESTED, timeline.getStatus());
        assertEquals(customer, timeline.getPerformedByActor());
        assertEquals(NOW, timeline.getOccurredAt());
    }

    @ParameterizedTest
    @MethodSource("nonCustomerRoles")
    void rejectsNonCustomerActors(ActorRole role) {
        assertReason("customer role", () -> QueueRequest.create(REQUEST_ID, actor(role), offering(true, true), SCHEDULED_FOR, 1, 0, NOW));
    }

    static Stream<ActorRole> nonCustomerRoles() {
        return Stream.of(ActorRole.RUNNER, ActorRole.ADMIN);
    }

    @Test
    void rejectsInactiveOffering() {
        assertReason("active", () -> QueueRequest.create(REQUEST_ID, actor(ActorRole.CUSTOMER), offering(false, true), SCHEDULED_FOR, 1, 0, NOW));
    }

    @Test
    void rejectsDelegationUnapprovedOffering() {
        assertReason("delegation", () -> QueueRequest.create(REQUEST_ID, actor(ActorRole.CUSTOMER), offering(true, false), SCHEDULED_FOR, 1, 0, NOW));
    }

    @Test
    void rejectsScheduleAtOrBeforeNow() {
        assertReason("scheduled", () -> QueueRequest.create(REQUEST_ID, actor(ActorRole.CUSTOMER), offering(true, true), NOW, 1, 0, NOW));
        assertReason("scheduled", () -> QueueRequest.create(REQUEST_ID, actor(ActorRole.CUSTOMER), offering(true, true), NOW.minusSeconds(1), 1, 0, NOW));
    }

    @ParameterizedTest
    @MethodSource("validNumericBoundaries")
    void acceptsInclusiveNumericBoundaries(int expectedQueueMinutes, int arrivalNoticeMinutes) {
        QueueRequest request = QueueRequest.create(REQUEST_ID, actor(ActorRole.CUSTOMER), offering(true, true), SCHEDULED_FOR,
                expectedQueueMinutes, arrivalNoticeMinutes, NOW);

        assertEquals(expectedQueueMinutes, request.getExpectedQueueMinutes());
        assertEquals(arrivalNoticeMinutes, request.getArrivalNoticeMinutes());
    }

    static Stream<Arguments> validNumericBoundaries() {
        return Stream.of(Arguments.of(1, 0), Arguments.of(720, 120));
    }

    @ParameterizedTest
    @MethodSource("invalidNumericValues")
    void rejectsNumericValuesOutsideBounds(int expectedQueueMinutes, int arrivalNoticeMinutes) {
        assertThrows(InvalidQueueRequestCreationException.class,
                () -> QueueRequest.create(REQUEST_ID, actor(ActorRole.CUSTOMER), offering(true, true), SCHEDULED_FOR,
                        expectedQueueMinutes, arrivalNoticeMinutes, NOW));
    }

    static Stream<Arguments> invalidNumericValues() {
        return Stream.of(Arguments.of(0, 0), Arguments.of(721, 0), Arguments.of(1, -1), Arguments.of(1, 121));
    }

    @Test
    void rejectsRequiredNullInputs() {
        Actor customer = actor(ActorRole.CUSTOMER);
        ServiceOffering offering = offering(true, true);
        assertThrows(InvalidQueueRequestCreationException.class, () -> QueueRequest.create(null, customer, offering, SCHEDULED_FOR, 1, 0, NOW));
        assertThrows(InvalidQueueRequestCreationException.class, () -> QueueRequest.create(REQUEST_ID, null, offering, SCHEDULED_FOR, 1, 0, NOW));
        assertThrows(InvalidQueueRequestCreationException.class, () -> QueueRequest.create(REQUEST_ID, customer, null, SCHEDULED_FOR, 1, 0, NOW));
        assertThrows(InvalidQueueRequestCreationException.class, () -> QueueRequest.create(REQUEST_ID, customer, offering, null, 1, 0, NOW));
        assertThrows(InvalidQueueRequestCreationException.class, () -> QueueRequest.create(REQUEST_ID, customer, offering, SCHEDULED_FOR, 1, 0, null));
        QueueRequest request = QueueRequest.create(REQUEST_ID, customer, offering, SCHEDULED_FOR, 1, 0, NOW);
        assertThrows(InvalidQueueRequestCreationException.class, () -> RequestTimelineEntry.initialFor(null, NOW));
        assertThrows(InvalidQueueRequestCreationException.class, () -> RequestTimelineEntry.initialFor(request, null));
    }

    private static Actor actor(ActorRole role) {
        Actor actor = org.mockito.Mockito.mock(Actor.class);
        org.mockito.Mockito.when(actor.getRole()).thenReturn(role);
        return actor;
    }

    private static ServiceOffering offering(boolean active, boolean delegationApproved) {
        ServiceOffering offering = org.mockito.Mockito.mock(ServiceOffering.class);
        org.mockito.Mockito.when(offering.isActive()).thenReturn(active);
        org.mockito.Mockito.when(offering.isDelegationApproved()).thenReturn(delegationApproved);
        return offering;
    }

    private static void assertReason(String expected, org.junit.jupiter.api.function.Executable executable) {
        InvalidQueueRequestCreationException exception = assertThrows(InvalidQueueRequestCreationException.class, executable);
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().toLowerCase().contains(expected));
    }
}
