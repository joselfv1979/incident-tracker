package com.jose.backend.comment;

import com.jose.backend.user.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Comments", description = "Discussion on an incident")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @Operation(
            summary = "List the comments of an incident",
            description = "Internal notes are only returned to agents and administrators.")
    public List<CommentResponse> list(@PathVariable Long incidentId, Authentication authentication) {
        return commentService.list(incidentId, CurrentUser.from(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add a comment",
            description = "Agents and administrators can mark it as an internal note.")
    @ApiResponse(responseCode = "403", description = "Regular users cannot write internal notes")
    public CommentResponse create(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        return commentService.create(incidentId, request, CurrentUser.from(authentication));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Edit your own comment")
    @ApiResponse(responseCode = "403", description = "Only the author can edit a comment")
    public CommentResponse update(
            @PathVariable Long incidentId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        return commentService.update(incidentId, commentId, request, CurrentUser.from(authentication));
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment", description = "Authors delete their own; administrators can delete any.")
    @ApiResponse(responseCode = "403", description = "Not the author or an administrator")
    public void delete(
            @PathVariable Long incidentId, @PathVariable Long commentId, Authentication authentication) {
        commentService.delete(incidentId, commentId, CurrentUser.from(authentication));
    }
}
