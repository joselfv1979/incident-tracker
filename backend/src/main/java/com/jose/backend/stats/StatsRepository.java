package com.jose.backend.stats;

import com.jose.backend.incident.Incident;
import com.jose.backend.incident.IncidentPriority;
import com.jose.backend.incident.IncidentStatus;
import com.jose.backend.user.Role;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StatsRepository extends Repository<Incident, Long> {

    interface StatusCount {
        IncidentStatus getStatus();

        long getTotal();
    }

    interface PriorityCount {
        IncidentPriority getPriority();

        long getTotal();
    }

    interface AssigneeLoad {
        Long getId();

        String getName();

        Role getRole();

        long getActive();
    }

    @Query("select i.status as status, count(i) as total from Incident i group by i.status")
    List<StatusCount> countByStatus();

    @Query("select i.priority as priority, count(i) as total from Incident i group by i.priority")
    List<PriorityCount> countByPriority();

    @Query("select count(i) from Incident i where i.assignee is null and i.status in :statuses")
    long countUnassigned(@Param("statuses") Collection<IncidentStatus> statuses);

    @Query("""
            select a.id as id, a.name as name, a.role as role, count(i) as active
            from Incident i join i.assignee a
            where i.status in :statuses
            group by a.id, a.name, a.role
            order by count(i) desc, a.name
            """)
    List<AssigneeLoad> activeByAssignee(@Param("statuses") Collection<IncidentStatus> statuses);

    // Rows are [day as 'YYYY-MM-DD' in UTC, count]
    @Query(
            value = """
                    SELECT to_char(created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day, COUNT(*) AS total
                    FROM incidents
                    WHERE created_at >= :since
                    GROUP BY 1
                    ORDER BY 1
                    """,
            nativeQuery = true)
    List<Object[]> createdPerDay(@Param("since") Instant since);

    @Query(
            value = """
                    SELECT to_char(resolved_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day, COUNT(*) AS total
                    FROM incidents
                    WHERE resolved_at IS NOT NULL AND resolved_at >= :since
                    GROUP BY 1
                    ORDER BY 1
                    """,
            nativeQuery = true)
    List<Object[]> resolvedPerDay(@Param("since") Instant since);

    // Rows are [priority, resolved count, average hours from creation to resolution]
    @Query(
            value = """
                    SELECT priority,
                           COUNT(*) AS total,
                           AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))) / 3600.0 AS hours
                    FROM incidents
                    WHERE resolved_at IS NOT NULL AND resolved_at >= :since
                    GROUP BY priority
                    """,
            nativeQuery = true)
    List<Object[]> resolutionTimes(@Param("since") Instant since);
}
