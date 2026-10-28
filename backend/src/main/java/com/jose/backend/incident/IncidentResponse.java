package com.jose.backend.incident;

import com.jose.backend.user.UserSummary;
import java.time.Instant;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        IncidentStatus status,
        IncidentPriority priority,
        String category,
        UserSummary reporter,
        UserSummary assignee,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt) {

    static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus(),
                incident.getPriority(),
                incident.getCategory(),
                UserSummary.from(incident.getReporter()),
                incident.getAssignee() == null ? null : UserSummary.from(incident.getAssignee()),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt());
    }
}
