package com.japtrack.project.exception.custom;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException (String message) {
        super(message);
    }
}
