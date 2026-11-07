package com.jose.backend.stats;

import java.util.List;

/** averageHours is null when no incident was resolved in the period. */
public record ResolutionTimeResponse(
        int days, long resolvedCount, Double averageHours, List<PriorityResolution> byPriority) {}
