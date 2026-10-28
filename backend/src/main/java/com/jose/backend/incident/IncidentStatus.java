package com.jose.backend.incident;

public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    IN_REVIEW,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(IncidentStatus next) {
        return switch (this) {
            case OPEN -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == IN_REVIEW || next == OPEN;
            case IN_REVIEW -> next == RESOLVED || next == IN_PROGRESS;
            case RESOLVED -> next == CLOSED || next == OPEN;
            case CLOSED -> next == OPEN;
        };
    }
}
