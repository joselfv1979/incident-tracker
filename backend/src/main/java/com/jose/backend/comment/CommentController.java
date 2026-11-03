package com.jose.backend.comment;

import com.jose.backend.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents/{incidentId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long incidentId, Authentication authentication) {
        return commentService.list(incidentId, CurrentUser.from(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        return commentService.create(incidentId, request, CurrentUser.from(authentication));
    }

    @PutMapping("/{commentId}")
    public CommentResponse update(
            @PathVariable Long incidentId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        return commentService.update(incidentId, commentId, request, CurrentUser.from(authentication));
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long incidentId, @PathVariable Long commentId, Authentication authentication) {
        commentService.delete(incidentId, commentId, CurrentUser.from(authentication));
    }
}
