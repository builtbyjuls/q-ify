package com.qify.identity.web;

public class ActorAuthenticationException extends RuntimeException {

    private final boolean missingIdentity;

    private ActorAuthenticationException(boolean missingIdentity) {
        this.missingIdentity = missingIdentity;
    }

    public static ActorAuthenticationException missingIdentity() {
        return new ActorAuthenticationException(true);
    }

    public static ActorAuthenticationException invalidIdentity() {
        return new ActorAuthenticationException(false);
    }

    public boolean isMissingIdentity() {
        return missingIdentity;
    }
}
