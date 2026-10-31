package com.jose.backend.incident;

import com.jose.backend.common.PageResponse;
import com.jose.backend.user.CurrentUser;
import com.jose.backend.user.OperationNotAllowedException;
import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class IncidentService {

    private static final int MAX_PAGE_SIZE = 100;
    // Closed list: the client cannot sort by any column it likes
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "title");

    private final IncidentRepository incidents;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public IncidentService(
            IncidentRepository incidents, UserRepository users, ApplicationEventPublisher events) {
        this.incidents = incidents;
        this.users = users;
        this.events = events;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request, CurrentUser actor) {
        Incident incident = new Incident();
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setCategory(normalizeCategory(request.category()));
        incident.setReporter(users.getReferenceById(actor.id()));
        Incident saved = incidents.save(incident);

        publish(saved, actor, ChangedField.CREATED, null, null);
        return IncidentResponse.from(saved);
    }

    public IncidentResponse get(Long id, CurrentUser actor) {
        return IncidentResponse.from(findVisible(id, actor));
    }

    /** Fails with "not found" when the incident does not exist or the user cannot see it. */
    public void requireVisible(Long id, CurrentUser actor) {
        findVisible(id, actor);
    }

    public PageResponse<IncidentResponse> search(
            IncidentFilter filter,
            int page,
            int size,
            String sortBy,
            String direction,
            CurrentUser actor) {
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        boolean descending;
        if ("desc".equalsIgnoreCase(direction)) {
            descending = true;
        } else if ("asc".equalsIgnoreCase(direction)) {
            descending = false;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction must be asc or desc");
        }

        Sort sort = Sort.by(
                descending ? Sort.Order.desc(sortBy) : Sort.Order.asc(sortBy), Sort.Order.desc("id"));
        Pageable pageable =
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);

        return PageResponse.from(
                incidents.findAll(IncidentSpecifications.from(filter, actor), pageable)
                        .map(IncidentResponse::from));
    }

    @Transactional
    public IncidentResponse update(Long id, UpdateIncidentRequest request, CurrentUser actor) {
        Incident incident = findVisible(id, actor);

        if (!actor.isStaff()) {
            if (incident.getStatus() != IncidentStatus.OPEN) {
                throw new OperationNotAllowedException(
                        "An incident can only be edited while it is open");
            }
            if (request.priority() != null) {
                throw new OperationNotAllowedException("Only agents can change the priority");
            }
        }

        String title = request.title().trim();
        String description = request.description().trim();
        String category = normalizeCategory(request.category());

        publishIfChanged(incident, actor, ChangedField.TITLE, incident.getTitle(), title);
        if (!Objects.equals(incident.getDescription(), description)) {
            // The text can be long: only the fact that it changed is recorded
            publish(incident, actor, ChangedField.DESCRIPTION, null, null);
        }
        publishIfChanged(incident, actor, ChangedField.CATEGORY, incident.getCategory(), category);
        if (request.priority() != null) {
            publishIfChanged(
                    incident,
                    actor,
                    ChangedField.PRIORITY,
                    incident.getPriority().name(),
                    request.priority().name());
        }

        incident.setTitle(title);
        incident.setDescription(description);
        incident.setCategory(category);
        if (request.priority() != null) {
            incident.setPriority(request.priority());
        }
        return IncidentResponse.from(incidents.saveAndFlush(incident));
    }

    @Transactional
    public IncidentResponse changeStatus(Long id, IncidentStatus next, CurrentUser actor) {
        Incident incident = findVisible(id, actor);
        IncidentStatus current = incident.getStatus();

        if (!actor.isStaff() && !(current == IncidentStatus.RESOLVED && next == IncidentStatus.OPEN)) {
            throw new OperationNotAllowedException("You can only reopen your own resolved incidents");
        }
        if (!current.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(current, next);
        }

        publish(incident, actor, ChangedField.STATUS, current.name(), next.name());

        incident.setStatus(next);
        if (next == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(Instant.now());
        } else if (next == IncidentStatus.OPEN) {
            incident.setResolvedAt(null);
        }
        return IncidentResponse.from(incidents.saveAndFlush(incident));
    }

    @Transactional
    public IncidentResponse assign(Long id, Long assigneeId, CurrentUser actor) {
        Incident incident = findVisible(id, actor);

        User assignee = null;
        if (assigneeId != null) {
            assignee = users.findById(assigneeId)
                    .filter(user -> user.getRole() != Role.USER)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "The assignee must be an agent or an administrator"));
        }

        User previous = incident.getAssignee();
        if (!Objects.equals(idOf(previous), idOf(assignee))) {
            publish(incident, actor, ChangedField.ASSIGNEE, nameOf(previous), nameOf(assignee));
        }

        incident.setAssignee(assignee);
        return IncidentResponse.from(incidents.saveAndFlush(incident));
    }

    /** Staff see every incident; users only theirs, and the others look like they do not exist. */
    private Incident findVisible(Long id, CurrentUser actor) {
        Incident incident = incidents.findWithPeopleById(id).orElseThrow(IncidentNotFoundException::new);
        if (!actor.isStaff() && !incident.getReporter().getId().equals(actor.id())) {
            throw new IncidentNotFoundException();
        }
        return incident;
    }

    private void publishIfChanged(
            Incident incident, CurrentUser actor, ChangedField field, String oldValue, String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            publish(incident, actor, field, oldValue, newValue);
        }
    }

    private void publish(
            Incident incident, CurrentUser actor, ChangedField field, String oldValue, String newValue) {
        events.publishEvent(new IncidentChanged(incident, actor.id(), field, oldValue, newValue));
    }

    private static Long idOf(User user) {
        return user == null ? null : user.getId();
    }

    private static String nameOf(User user) {
        return user == null ? null : user.getName();
    }

    private static String normalizeCategory(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }
}
