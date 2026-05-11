package com.example.retail.web.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error envelope")
public record ErrorResponse(
    @Schema(example = "404") int status,
    @Schema(example = "Not Found") String error,
    String message,
    List<String> details,
    Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(int status, String error, String message, List<String> details) {
        return new ErrorResponse(status, error, message, details, Instant.now());
    }
}
