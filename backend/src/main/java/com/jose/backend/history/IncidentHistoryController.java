package com.jose.backend.history;

import com.jose.backend.user.CurrentUser;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents/{incidentId}/history")
public class IncidentHistoryController {

    private final IncidentHistoryService historyService;

    public IncidentHistoryController(IncidentHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<HistoryEntryResponse> list(
            @PathVariable Long incidentId, Authentication authentication) {
        return historyService.list(incidentId, CurrentUser.from(authentication));
    }
}
