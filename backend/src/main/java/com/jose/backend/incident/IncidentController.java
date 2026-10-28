package com.jose.backend.incident;

import com.jose.backend.common.PageResponse;
import com.jose.backend.user.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(
            @Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        return incidentService.create(request, CurrentUser.from(authentication));
    }

    @GetMapping
    public PageResponse<IncidentResponse> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate createdTo,
            @RequestParam(name = "q", required = false) String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {
        IncidentFilter filter = new IncidentFilter(
                status, priority, assigneeId, category, createdFrom, createdTo, text);
        return incidentService.search(
                filter, page, size, sortBy, direction, CurrentUser.from(authentication));
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id, Authentication authentication) {
        return incidentService.get(id, CurrentUser.from(authentication));
    }

    @PutMapping("/{id}")
    public IncidentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request,
            Authentication authentication) {
        return incidentService.update(id, request, CurrentUser.from(authentication));
    }

    @PatchMapping("/{id}/status")
    public IncidentResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication) {
        return incidentService.changeStatus(id, request.status(), CurrentUser.from(authentication));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public IncidentResponse assign(
            @PathVariable Long id, @RequestBody AssignRequest request, Authentication authentication) {
        return incidentService.assign(id, request.assigneeId(), CurrentUser.from(authentication));
    }
}
