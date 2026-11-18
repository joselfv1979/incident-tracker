package com.jose.backend.incident;

import com.jose.backend.common.PageResponse;
import com.jose.backend.user.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Incidents", description = "Report, find, edit and resolve incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Report an incident",
            description = "Created as OPEN with MEDIUM priority.")
    public IncidentResponse create(
            @Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        return incidentService.create(request, CurrentUser.from(authentication));
    }

    @GetMapping
    @Operation(
            summary = "List incidents",
            description = "Filter by status, priority, assignee, category, creation dates and text (q). "
            + "Users only see their own incidents; agents and administrators see all of them.")
    @ApiResponse(responseCode = "400", description = "Unsupported sort field or direction")
    public PageResponse<IncidentResponse> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
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
    @Operation(summary = "Get an incident")
    @ApiResponse(responseCode = "404", description = "It does not exist, or it belongs to someone else")
    public IncidentResponse get(@PathVariable Long id, Authentication authentication) {
        return incidentService.get(id, CurrentUser.from(authentication));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Edit an incident",
            description = "Full replacement of title, description and category. Reporters can edit "
            + "only while it is OPEN; only staff can change the priority.")
    @ApiResponse(responseCode = "403", description = "Not allowed to edit it in its current state")
    @ApiResponse(responseCode = "404", description = "Incident not found")
    public IncidentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request,
            Authentication authentication) {
        return incidentService.update(id, request, CurrentUser.from(authentication));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Change the status",
            description = "OPEN → IN_PROGRESS → IN_REVIEW → RESOLVED → CLOSED, with moves back and "
            + "reopening. Reporters can only reopen their own resolved incidents.")
    @ApiResponse(responseCode = "403", description = "Not allowed to make this change")
    @ApiResponse(responseCode = "409", description = "The transition is not allowed")
    public IncidentResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication) {
        return incidentService.changeStatus(id, request.status(), CurrentUser.from(authentication));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(
            summary = "Assign or unassign an incident (agents and administrators)",
            description = "A null assigneeId removes the current assignee.")
    @ApiResponse(responseCode = "400", description = "The assignee must be an agent or an administrator")
    public IncidentResponse assign(
            @PathVariable Long id, @RequestBody AssignRequest request, Authentication authentication) {
        return incidentService.assign(id, request.assigneeId(), CurrentUser.from(authentication));
    }
}
