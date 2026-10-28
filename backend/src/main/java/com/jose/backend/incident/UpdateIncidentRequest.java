package com.jose.backend.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Full replacement of the editable fields. The priority is optional and staff only. */
public record UpdateIncidentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 50) String category,
        IncidentPriority priority) {}
