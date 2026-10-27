// domain/exception/IllegalStateTransitionException.java
package com.phuchoccode.OrderService.domain.exception;

public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
