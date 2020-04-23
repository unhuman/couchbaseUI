package com.unhuman.couchbaseui.exceptions;

/**
 * This exception should be used when there's some invalid
 */
public class BadInputException extends RuntimeException {
    public BadInputException(String description) {
        super(description);
    }
}
