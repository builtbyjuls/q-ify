package com.qify.fulfillment.web;

import java.time.Instant;
import java.util.UUID;

import com.qify.catalog.domain.ServiceCategory;
import com.qify.fulfillment.domain.QueueRequestStatus;

public record QueueRequestResponse(
        UUID id,
        UUID serviceOfferingId,
        UUID venueId,
        String venueName,
        ServiceCategory serviceCategory,
        QueueRequestStatus status,
        Instant scheduledFor,
        int expectedQueueMinutes,
        int arrivalNoticeMinutes,
        Instant createdAt) {
}
