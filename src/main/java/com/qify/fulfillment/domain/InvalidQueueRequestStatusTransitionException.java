package com.qify.fulfillment.domain;

public class InvalidQueueRequestStatusTransitionException extends IllegalStateException {

    public InvalidQueueRequestStatusTransitionException(QueueRequestStatus source, QueueRequestStatus target) {
        super("Invalid queue request status transition from " + source + " to " + target);
    }
}
