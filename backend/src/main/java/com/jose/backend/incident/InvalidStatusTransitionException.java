package com.jose.backend.incident;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(IncidentStatus from, IncidentStatus to) {
        super("Cannot change the status from " + from + " to " + to);
    }
}
