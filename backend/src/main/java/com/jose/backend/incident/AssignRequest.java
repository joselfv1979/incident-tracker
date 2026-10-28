package com.jose.backend.incident;

/** A null assignee removes the current one. */
public record AssignRequest(Long assigneeId) {}
