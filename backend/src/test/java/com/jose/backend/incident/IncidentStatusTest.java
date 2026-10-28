package com.jose.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IncidentStatusTest {

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED = Map.of(
            IncidentStatus.OPEN, EnumSet.of(IncidentStatus.IN_PROGRESS),
            IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentStatus.IN_REVIEW, IncidentStatus.OPEN),
            IncidentStatus.IN_REVIEW, EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.IN_PROGRESS),
            IncidentStatus.RESOLVED, EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.OPEN),
            IncidentStatus.CLOSED, EnumSet.of(IncidentStatus.OPEN));

    @Test
    void onlyTheDocumentedTransitionsAreAllowed() {
        for (IncidentStatus from : IncidentStatus.values()) {
            for (IncidentStatus to : IncidentStatus.values()) {
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(ALLOWED.get(from).contains(to));
            }
        }
    }
}
