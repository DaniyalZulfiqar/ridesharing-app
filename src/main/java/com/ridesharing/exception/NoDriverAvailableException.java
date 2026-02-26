package com.ridesharing.exception;

public class NoDriverAvailableException extends RuntimeException {

    public NoDriverAvailableException() {
        super("No available drivers at the moment. Please try again later.");
    }
}
