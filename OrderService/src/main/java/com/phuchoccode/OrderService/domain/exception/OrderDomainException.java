// domain/exception/OrderDomainException.java
package com.phuchoccode.OrderService.domain.exception;

public class OrderDomainException extends RuntimeException {

    public OrderDomainException(String message) {
        super(message);
    }

    public OrderDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}