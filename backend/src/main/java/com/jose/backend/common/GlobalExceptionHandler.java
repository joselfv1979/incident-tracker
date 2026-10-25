package com.jose.backend.common;

import com.jose.backend.auth.EmailNotVerifiedException;
import com.jose.backend.auth.InvalidCredentialsException;
import com.jose.backend.auth.InvalidVerificationTokenException;

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
}
