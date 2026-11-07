package com.jose.backend.stats;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/summary")
    public StatsSummary summary() {
        return statsService.summary();
    }

    @GetMapping("/trend")
    public TrendResponse trend(@RequestParam(defaultValue = "30") int days) {
        return statsService.trend(days);
    }

    @GetMapping("/resolution-time")
    public ResolutionTimeResponse resolutionTime(@RequestParam(defaultValue = "90") int days) {
        return statsService.resolutionTime(days);
    }
}
