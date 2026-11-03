package com.jose.backend.comment;

import com.jose.backend.incident.IncidentRepository;
import com.jose.backend.incident.IncidentService;
import com.jose.backend.user.CurrentUser;
import com.jose.backend.user.OperationNotAllowedException;
import com.jose.backend.user.Role;
import com.jose.backend.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository comments;
    private final IncidentService incidentService;
    private final IncidentRepository incidents;
    private final UserRepository users;

    public CommentService(
            CommentRepository comments,
            IncidentService incidentService,
            IncidentRepository incidents,
            UserRepository users) {
        this.comments = comments;
        this.incidentService = incidentService;
        this.incidents = incidents;
        this.users = users;
    }

    public List<CommentResponse> list(Long incidentId, CurrentUser actor) {
        incidentService.requireVisible(incidentId, actor);
        List<Comment> found = actor.isStaff()
                ? comments.findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId)
                : comments.findByIncidentIdAndInternalFalseOrderByCreatedAtAscIdAsc(incidentId);
        return found.stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse create(Long incidentId, CreateCommentRequest request, CurrentUser actor) {
        incidentService.requireVisible(incidentId, actor);

        boolean internal = Boolean.TRUE.equals(request.internal());
        if (internal && !actor.isStaff()) {
            throw new OperationNotAllowedException("Only agents can write internal notes");
        }

        Comment comment = new Comment();
        comment.setIncident(incidents.getReferenceById(incidentId));
        comment.setAuthor(users.getReferenceById(actor.id()));
        comment.setBody(request.body().trim());
        comment.setInternal(internal);
        return CommentResponse.from(comments.save(comment));
    }

    @Transactional
    public CommentResponse update(
            Long incidentId, Long commentId, UpdateCommentRequest request, CurrentUser actor) {
        Comment comment = findVisible(incidentId, commentId, actor);
        if (!comment.getAuthor().getId().equals(actor.id())) {
            throw new OperationNotAllowedException("You can only edit your own comments");
        }

        String body = request.body().trim();
        if (!body.equals(comment.getBody())) {
            comment.setBody(body);
            comment.setEditedAt(Instant.now());
        }
        return CommentResponse.from(comments.saveAndFlush(comment));
    }

    @Transactional
    public void delete(Long incidentId, Long commentId, CurrentUser actor) {
        Comment comment = findVisible(incidentId, commentId, actor);
        boolean own = comment.getAuthor().getId().equals(actor.id());
        if (!own && actor.role() != Role.ADMIN) {
            throw new OperationNotAllowedException("You can only delete your own comments");
        }
        comments.delete(comment);
    }

    /**
     * The comment must belong to that incident, the incident must be visible to the user,
     * and internal notes look like they do not exist for anybody but the staff.
     */
    private Comment findVisible(Long incidentId, Long commentId, CurrentUser actor) {
        incidentService.requireVisible(incidentId, actor);
        Comment comment = comments.findByIdAndIncidentId(commentId, incidentId)
                .orElseThrow(CommentNotFoundException::new);
        if (comment.isInternal() && !actor.isStaff()) {
            throw new CommentNotFoundException();
        }
        return comment;
    }
}
