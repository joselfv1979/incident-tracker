package com.jose.backend.stats;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@Tag(name = "Statistics", description = "Aggregated data for agents and administrators")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Totals by status, priority and assignee")
    public StatsSummary summary() {
        return statsService.summary();
    }

    @GetMapping("/trend")
    @Operation(summary = "Incidents created and resolved per day (UTC), up to 365 days")
    @ApiResponse(responseCode = "400", description = "days must be between 1 and 365")
    public TrendResponse trend(@RequestParam(defaultValue = "30") int days) {
        return statsService.trend(days);
    }

    @GetMapping("/resolution-time")
    @Operation(summary = "Average time from creation to resolution, overall and by priority")
    public ResolutionTimeResponse resolutionTime(@RequestParam(defaultValue = "90") int days) {
        return statsService.resolutionTime(days);
    }
}
