package com.jose.backend.history;

import com.jose.backend.incident.IncidentService;
import com.jose.backend.user.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IncidentHistoryService {

    private final IncidentHistoryRepository history;
    private final IncidentService incidentService;

    public IncidentHistoryService(IncidentHistoryRepository history, IncidentService incidentService) {
        this.history = history;
        this.incidentService = incidentService;
    }

    public List<HistoryEntryResponse> list(Long incidentId, CurrentUser actor) {
        // Same visibility as the incident itself
        incidentService.requireVisible(incidentId, actor);
        return history.findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId).stream()
                .map(HistoryEntryResponse::from)
                .toList();
    }
}
