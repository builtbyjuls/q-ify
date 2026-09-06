package com.qify.fulfillment.application;

public class QueueRequestCreationResourceNotFoundException extends IllegalArgumentException {

    public QueueRequestCreationResourceNotFoundException(String detail) {
        super(detail);
    }
}
