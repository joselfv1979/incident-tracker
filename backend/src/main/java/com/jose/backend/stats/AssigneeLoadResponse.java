package com.jose.backend.stats;

import com.jose.backend.user.UserSummary;

public record AssigneeLoadResponse(UserSummary user, long active) {}
