package com.retailer.rewards.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uniform error body returned for every failure, so clients only have to parse one shape.
 */
@Schema(description = "Standard error payload")
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    @Schema(example = "400")
    private final int status;

    @Schema(example = "Bad Request")
    private final String error;

    @Schema(example = "startDate must not be after endDate")
    private final String message;

    @Schema(example = "/api/v1/rewards/customers/1")
    private final String path;

    @Schema(description = "Field level problems, present only for validation failures")
    private final List<String> details;

    public ErrorResponse(int status, String error, String message, String path,
                         List<String> details) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = (details == null || details.isEmpty()) ? null : details;
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, path, null);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<String> getDetails() {
        return details;
    }
}
