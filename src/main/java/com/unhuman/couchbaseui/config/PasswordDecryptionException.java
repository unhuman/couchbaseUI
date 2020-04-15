package com.unhuman.couchbaseui.config;

public class PasswordDecryptionException extends RuntimeException {
    PasswordDecryptionException(String description) {
        super(description);
    }
}
