package com.jose.backend.incident;

import com.jose.backend.user.CurrentUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class IncidentSpecifications {

    private IncidentSpecifications() {}

    static Specification<Incident> from(IncidentFilter filter, CurrentUser actor) {
        Specification<Incident> spec = (root, query, cb) -> cb.conjunction();

        // Users only ever see the incidents they reported
        if (!actor.isStaff()) {
            spec = spec.and(reportedBy(actor.id()));
        }
        if (filter.status() != null) {
            spec = spec.and(hasStatus(filter.status()));
        }
        if (filter.priority() != null) {
            spec = spec.and(hasPriority(filter.priority()));
        }
        if (filter.assigneeId() != null) {
            spec = spec.and(assignedTo(filter.assigneeId()));
        }
        if (filter.category() != null && !filter.category().isBlank()) {
            spec = spec.and(hasCategory(filter.category()));
        }
        if (filter.createdFrom() != null) {
            spec = spec.and(createdFrom(filter.createdFrom()));
        }
        if (filter.createdTo() != null) {
            spec = spec.and(createdTo(filter.createdTo()));
        }
        if (filter.text() != null && !filter.text().isBlank()) {
            spec = spec.and(containsText(filter.text()));
        }
        return spec;
    }

    private static Specification<Incident> reportedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("reporter").get("id"), userId);
    }

    private static Specification<Incident> hasStatus(IncidentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Incident> hasPriority(IncidentPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    private static Specification<Incident> assignedTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }

    private static Specification<Incident> hasCategory(String category) {
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("category")), normalized);
    }

    private static Specification<Incident> createdFrom(LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), start);
    }

    // Inclusive: everything created before the start of the following day
    private static Specification<Incident> createdTo(LocalDate date) {
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return (root, query, cb) -> cb.lessThan(root.<Instant>get("createdAt"), end);
    }

    private static Specification<Incident> containsText(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                cb.like(cb.lower(root.<String>get("description")), pattern, '\\'));
    }

    // The search text is data, so its LIKE wildcards must not act as wildcards
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
