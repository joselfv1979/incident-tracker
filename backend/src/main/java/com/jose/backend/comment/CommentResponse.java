package com.jose.backend.comment;

import com.jose.backend.user.UserSummary;
import java.time.Instant;

public record CommentResponse(
        Long id,
        String body,
        boolean internal,
        UserSummary author,
        Instant createdAt,
        Instant editedAt) {

    static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.isInternal(),
                UserSummary.from(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getEditedAt());
    }
}
