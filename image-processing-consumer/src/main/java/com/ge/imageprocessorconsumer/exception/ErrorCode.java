package com.ge.imageprocessorconsumer.exception;

/**
 * Enum for categorizing error types.
 */
public enum ErrorCode {
    FILE_NOT_FOUND("FILE_NOT_FOUND", "The specified image file was not found"),
    INVALID_FILE("INVALID_FILE", "The image file is invalid or corrupted"),
    PROCESSING_FAILURE("PROCESSING_FAILURE", "Failed to process the image"),
    IO_ERROR("IO_ERROR", "An I/O error occurred during image processing"),
    CONFIGURATION_ERROR("CONFIGURATION_ERROR", "Invalid configuration for image processing");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
