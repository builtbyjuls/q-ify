package com.qify.fulfillment.domain;

public enum QueueRequestStatus {
    REQUESTED,
    ASSIGNED,
    ARRIVED,
    IN_QUEUE,
    HANDOFF_READY,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(QueueRequestStatus target) {
        if (target == null) {
            return false;
        }

        return switch (this) {
            case REQUESTED -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == ARRIVED || target == CANCELLED;
            case ARRIVED -> target == IN_QUEUE || target == CANCELLED;
            case IN_QUEUE -> target == HANDOFF_READY || target == CANCELLED;
            case HANDOFF_READY -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public void requireTransitionTo(QueueRequestStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidQueueRequestStatusTransitionException(this, target);
        }
    }
}
