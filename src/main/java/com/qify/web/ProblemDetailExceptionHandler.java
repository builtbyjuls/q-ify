package com.qify.web;

import com.qify.fulfillment.application.QueueRequestCreationResourceNotFoundException;
import com.qify.fulfillment.domain.InvalidQueueRequestCreationException;
import com.qify.identity.web.ActorAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ProblemDetailExceptionHandler {

    @ExceptionHandler(ActorAuthenticationException.class)
    ProblemDetail handleActorAuthentication(ActorAuthenticationException exception) {
        String detail = exception.isMissingIdentity()
                ? "Actor identity is required."
                : "Actor identity is invalid.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidArguments(MethodArgumentNotValidException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is invalid.");
    }

    @ExceptionHandler(QueueRequestCreationResourceNotFoundException.class)
    ProblemDetail handleCreationResourceNotFound(QueueRequestCreationResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidQueueRequestCreationException.class)
    ProblemDetail handleInvalidQueueRequestCreation(InvalidQueueRequestCreationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getReason());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatus(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(), exception.getReason());
    }
}
