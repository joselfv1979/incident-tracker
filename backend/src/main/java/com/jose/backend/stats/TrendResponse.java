package com.jose.backend.stats;

import java.util.List;

public record TrendResponse(int days, List<TrendPoint> points) {}
