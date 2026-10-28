package com.jose.backend.common;

import com.jose.backend.auth.EmailNotVerifiedException;
import com.jose.backend.auth.InvalidCredentialsException;
import com.jose.backend.auth.InvalidVerificationTokenException;
import com.jose.backend.incident.IncidentNotFoundException;
import com.jose.backend.incident.InvalidStatusTransitionException;
import com.jose.backend.user.OperationNotAllowedException;
import com.jose.backend.user.UserNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail handleEmailNotVerified(EmailNotVerifiedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    ProblemDetail handleInvalidToken(InvalidVerificationTokenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    ProblemDetail handleNotAllowed(OperationNotAllowedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    ProblemDetail handleIncidentNotFound(IncidentNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidStatusTransitionException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
