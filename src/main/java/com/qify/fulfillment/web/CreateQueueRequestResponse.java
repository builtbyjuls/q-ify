package com.qify.fulfillment.web;

import java.util.UUID;

import com.qify.fulfillment.domain.QueueRequestStatus;

public record CreateQueueRequestResponse(UUID id, QueueRequestStatus status) {
}
