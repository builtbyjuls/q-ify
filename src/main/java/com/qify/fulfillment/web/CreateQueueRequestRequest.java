package com.qify.fulfillment.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateQueueRequestRequest(
        @NotNull UUID serviceOfferingId,
        @NotNull OffsetDateTime scheduledFor,
        @NotNull @Min(1) @Max(720) Integer expectedQueueMinutes,
        @NotNull @Min(0) @Max(120) Integer arrivalNoticeMinutes
) {
}
