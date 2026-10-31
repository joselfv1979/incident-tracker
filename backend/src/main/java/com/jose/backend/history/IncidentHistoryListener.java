package com.jose.backend.history;

import com.jose.backend.incident.IncidentChanged;
import com.jose.backend.user.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentHistoryListener {

    private static final int MAX_VALUE_LENGTH = 255;

    private final IncidentHistoryRepository history;
    private final UserRepository users;

    public IncidentHistoryListener(IncidentHistoryRepository history, UserRepository users) {
        this.history = history;
        this.users = users;
    }

    // A plain listener runs in the publisher's transaction: if the change is rolled
    // back, so is its history entry.
    @EventListener
    public void on(IncidentChanged event) {
        IncidentHistory entry = new IncidentHistory();
        entry.setIncident(event.incident());
        entry.setActor(users.getReferenceById(event.actorId()));
        entry.setField(event.field().name());
        entry.setOldValue(abbreviate(event.oldValue()));
        entry.setNewValue(abbreviate(event.newValue()));
        history.save(entry);
    }

    private static String abbreviate(String value) {
        if (value == null || value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VALUE_LENGTH - 1) + "…";
    }
}
