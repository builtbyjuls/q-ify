package com.qify.web;

import com.qify.identity.web.ActorAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemDetailExceptionHandler {

    @ExceptionHandler(ActorAuthenticationException.class)
    ProblemDetail handleActorAuthentication(ActorAuthenticationException exception) {
        String detail = exception.isMissingIdentity()
                ? "Actor identity is required."
                : "Actor identity is invalid.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    }
}
