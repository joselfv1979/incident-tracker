package com.jose.backend.stats;

import com.jose.backend.incident.IncidentPriority;
import com.jose.backend.incident.IncidentStatus;
import java.util.List;
import java.util.Map;

public record StatsSummary(
        long total,
        long active,
        long unassigned,
        Map<IncidentStatus, Long> byStatus,
        Map<IncidentPriority, Long> byPriority,
        List<AssigneeLoadResponse> byAssignee) {}
