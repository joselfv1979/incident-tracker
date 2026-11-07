package com.jose.backend.stats;

import com.jose.backend.incident.IncidentPriority;
import com.jose.backend.incident.IncidentStatus;
import com.jose.backend.user.UserSummary;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class StatsService {

    /** Work still to be done: resolved and closed incidents do not count. */
    private static final Set<IncidentStatus> ACTIVE =
            EnumSet.of(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS, IncidentStatus.IN_REVIEW);

    private static final int MAX_DAYS = 365;

    private final StatsRepository stats;

    public StatsService(StatsRepository stats) {
        this.stats = stats;
    }

    public StatsSummary summary() {
        Map<IncidentStatus, Long> byStatus = new EnumMap<>(IncidentStatus.class);
        for (IncidentStatus status : IncidentStatus.values()) {
            byStatus.put(status, 0L);
        }
        stats.countByStatus().forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));

        Map<IncidentPriority, Long> byPriority = new EnumMap<>(IncidentPriority.class);
        for (IncidentPriority priority : IncidentPriority.values()) {
            byPriority.put(priority, 0L);
        }
        stats.countByPriority().forEach(row -> byPriority.put(row.getPriority(), row.getTotal()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long active = ACTIVE.stream().mapToLong(status -> byStatus.get(status)).sum();

        List<AssigneeLoadResponse> byAssignee = stats.activeByAssignee(ACTIVE).stream()
                .map(row -> new AssigneeLoadResponse(
                        new UserSummary(row.getId(), row.getName(), row.getRole()), row.getActive()))
                .toList();

        return new StatsSummary(
                total, active, stats.countUnassigned(ACTIVE), byStatus, byPriority, byAssignee);
    }

    public TrendResponse trend(int days) {
        validateDays(days);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = today.minusDays(days - 1L);
        Instant since = first.atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<LocalDate, Long> created = perDay(stats.createdPerDay(since));
        Map<LocalDate, Long> resolved = perDay(stats.resolvedPerDay(since));

        List<TrendPoint> points = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(today); day = day.plusDays(1)) {
            points.add(new TrendPoint(day, created.getOrDefault(day, 0L), resolved.getOrDefault(day, 0L)));
        }
        return new TrendResponse(days, points);
    }

    public ResolutionTimeResponse resolutionTime(int days) {
        validateDays(days);
        Instant since = Instant.now().minus(Duration.ofDays(days));

        List<PriorityResolution> byPriority = new ArrayList<>();
        long count = 0;
        double hoursSum = 0;
        for (Object[] row : stats.resolutionTimes(since)) {
            long rowCount = ((Number) row[1]).longValue();
            double average = ((Number) row[2]).doubleValue();
            byPriority.add(new PriorityResolution(
                    IncidentPriority.valueOf((String) row[0]), rowCount, round(average)));
            count += rowCount;
            hoursSum += average * rowCount;
        }
        byPriority.sort(Comparator.comparing(PriorityResolution::priority));

        Double average = count == 0 ? null : round(hoursSum / count);
        return new ResolutionTimeResponse(days, count, average, byPriority);
    }

    private static Map<LocalDate, Long> perDay(List<Object[]> rows) {
        Map<LocalDate, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(LocalDate.parse((String) row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    private static void validateDays(int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "days must be between 1 and " + MAX_DAYS);
        }
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
