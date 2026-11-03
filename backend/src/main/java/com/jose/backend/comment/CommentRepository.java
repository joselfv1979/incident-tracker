package com.jose.backend.comment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByIncidentIdOrderByCreatedAtAscIdAsc(Long incidentId);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByIncidentIdAndInternalFalseOrderByCreatedAtAscIdAsc(Long incidentId);

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findByIdAndIncidentId(Long id, Long incidentId);
}
