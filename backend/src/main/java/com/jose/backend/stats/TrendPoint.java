package com.jose.backend.stats;

import java.time.LocalDate;

public record TrendPoint(LocalDate date, long created, long resolved) {}
