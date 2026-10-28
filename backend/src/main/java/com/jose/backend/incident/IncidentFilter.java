package com.jose.backend.incident;

import java.time.LocalDate;

public record IncidentFilter(
        IncidentStatus status,
        IncidentPriority priority,
        Long assigneeId,
        String category,
        LocalDate createdFrom,
        LocalDate createdTo,
        String text) {}
