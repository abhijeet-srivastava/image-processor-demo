package com.ge.imageprocessorconsumer.exception;

import java.io.Serializable;

/**
 * Custom exception for image processing errors in the image processing pipeline.
 * Supports error codes, messages, and root causes for detailed error handling.
 * Serializable to ensure compatibility with Kafka messaging.
 */
public class ImageProcessingException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String details;

    /**
     * Constructs an ImageProcessingException with an error code and message.
     *
     * @param errorCode The error code categorizing the exception.
     * @param message   The error message.
     */
    public ImageProcessingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = message;
    }

    /**
     * Constructs an ImageProcessingException with an error code, message, and cause.
     *
     * @param errorCode The error code categorizing the exception.
     * @param message   The error message.
     * @param cause     The root cause of the exception.
     */
    public ImageProcessingException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = message;
    }

    /**
     * Constructs an ImageProcessingException with an error code and cause.
     *
     * @param errorCode The error code categorizing the exception.
     * @param cause     The root cause of the exception.
     */
    public ImageProcessingException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.details = cause.getMessage();
    }

    /**
     * Gets the error code.
     *
     * @return The error code.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the detailed error message.
     *
     * @return The detailed error message.
     */
    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("ImageProcessingException{errorCode=%s, details='%s', message='%s'}",
                errorCode.getCode(), details, getMessage());
    }
}
