package com.jose.backend.incident;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentRepository
        extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    @EntityGraph(attributePaths = {"reporter", "assignee"})
    Optional<Incident> findWithPeopleById(Long id);

    @Override
    @EntityGraph(attributePaths = {"reporter", "assignee"})
    Page<Incident> findAll(Specification<Incident> spec, Pageable pageable);
}
