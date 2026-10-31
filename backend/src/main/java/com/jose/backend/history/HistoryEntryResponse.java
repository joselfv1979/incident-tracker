package com.jose.backend.history;

import com.jose.backend.user.UserSummary;
import java.time.Instant;

public record HistoryEntryResponse(
        Long id, String field, String oldValue, String newValue, UserSummary actor, Instant createdAt) {

    static HistoryEntryResponse from(IncidentHistory entry) {
        return new HistoryEntryResponse(
                entry.getId(),
                entry.getField(),
                entry.getOldValue(),
                entry.getNewValue(),
                UserSummary.from(entry.getActor()),
                entry.getCreatedAt());
    }
}
