package com.qify.fulfillment.domain;

public class InvalidQueueRequestCreationException extends IllegalArgumentException {

    private final String reason;

    public InvalidQueueRequestCreationException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
