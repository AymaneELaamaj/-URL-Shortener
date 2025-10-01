package com.project.URL.Shortener.config;


import lombok.Data;

@Data
public class RateLimitConfig {
    private final int maxRequests;
    private final long windowMs;

    public static RateLimitConfig perMinute(int requests) {
        return new RateLimitConfig(requests, 60_000);
    }

    public static RateLimitConfig perHour(int requests) {
        return new RateLimitConfig(requests, 3_600_000);
    }
}
