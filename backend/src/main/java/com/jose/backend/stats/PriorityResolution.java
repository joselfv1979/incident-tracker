package com.jose.backend.stats;

import com.jose.backend.incident.IncidentPriority;

public record PriorityResolution(IncidentPriority priority, long resolvedCount, double averageHours) {}
