package com.jose.backend.incident;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull IncidentStatus status) {}
