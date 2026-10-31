package com.jose.backend.history;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {

    @EntityGraph(attributePaths = "actor")
    List<IncidentHistory> findByIncidentIdOrderByCreatedAtAscIdAsc(Long incidentId);
}
