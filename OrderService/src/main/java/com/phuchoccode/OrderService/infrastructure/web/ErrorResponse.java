package com.phuchoccode.OrderService.infrastructure.web;

import java.time.Instant;

// infrastructure/web/ErrorResponse.java
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {}
