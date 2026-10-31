package com.jose.backend.incident;

/** Published inside the transaction that changes the incident. */
public record IncidentChanged(
        Incident incident, Long actorId, ChangedField field, String oldValue, String newValue) {}
