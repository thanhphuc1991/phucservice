package com.phuchoccode.OrderService.infrastructure.web;

import com.phuchoccode.OrderService.domain.exception.IllegalStateTransitionException;
import com.phuchoccode.OrderService.domain.exception.OrderDomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(
            IllegalStateTransitionException ex) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "INVALID_STATE_TRANSITION",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(OrderDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(
            OrderDomainException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "DOMAIN_ERROR",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        ex.getMessage(),
                        Instant.now()
                ));
    }
}