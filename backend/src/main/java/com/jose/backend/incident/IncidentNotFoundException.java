package com.jose.backend.incident;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException() {
        super("Incident not found");
    }
}
