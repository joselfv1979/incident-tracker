package com.jose.backend.history;

import com.jose.backend.user.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents/{incidentId}/history")
@Tag(name = "History", description = "Record of the changes made to an incident")
public class IncidentHistoryController {

    private final IncidentHistoryService historyService;

    public IncidentHistoryController(IncidentHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "List the changes of an incident, oldest first")
    @ApiResponse(responseCode = "404", description = "It does not exist, or it belongs to someone else")
    public List<HistoryEntryResponse> list(
            @PathVariable Long incidentId, Authentication authentication) {
        return historyService.list(incidentId, CurrentUser.from(authentication));
    }
}
