package com.jose.backend.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The internal flag is optional and only agents and administrators may set it. */
public record CreateCommentRequest(@NotBlank @Size(max = 5000) String body, Boolean internal) {}
