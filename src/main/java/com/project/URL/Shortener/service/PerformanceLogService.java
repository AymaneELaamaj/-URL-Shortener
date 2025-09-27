package com.project.URL.Shortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class PerformanceLogService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceLogService.class);

    // Cache Operations
    public void logCacheHit(String shortCode, String operation) {
        MDC.put("operation", operation);
        MDC.put("shortCode", shortCode);
        logger.info("CACHE_HIT - Retrieved from Redis cache");
        MDC.clear();
    }

    public void logCacheMiss(String shortCode, String operation) {
        MDC.put("operation", operation);
        MDC.put("shortCode", shortCode);
        logger.info("CACHE_MISS - Not found in Redis cache");
        MDC.clear();
    }

    public void logCacheStore(String shortCode, String originalUrl) {
        MDC.put("operation", "CACHE_STORE");
        MDC.put("shortCode", shortCode);
        logger.info("CACHE_STORE - Stored URL in Redis cache: {}", originalUrl);
        MDC.clear();
    }

    // Database Operations
    public void logDatabaseQuery(String shortCode, String queryType, long executionTimeMs) {
        MDC.put("operation", "DB_QUERY");
        MDC.put("queryType", queryType);
        MDC.put("shortCode", shortCode);
        MDC.put("executionTime", String.valueOf(executionTimeMs));
        logger.info("DB_QUERY - {} executed in {}ms", queryType, executionTimeMs);
        MDC.clear();
    }

    public void logDatabaseSave(String shortCode, String originalUrl, long executionTimeMs) {
        MDC.put("operation", "DB_SAVE");
        MDC.put("shortCode", shortCode);
        MDC.put("executionTime", String.valueOf(executionTimeMs));
        logger.info("DB_SAVE - Saved new URL mapping in {}ms: {}", executionTimeMs, originalUrl);
        MDC.clear();
    }

    // Request Flow
    public void logRequestStart(String endpoint, String shortCode, String clientIp) {
        MDC.put("operation", "REQUEST_START");
        MDC.put("endpoint", endpoint);
        MDC.put("shortCode", shortCode);
        MDC.put("clientIp", clientIp);
        logger.info("REQUEST_START - {} request for shortCode: {}", endpoint, shortCode);
        MDC.clear();
    }

    public void logRequestComplete(String endpoint, String shortCode, long totalTimeMs) {
        MDC.put("operation", "REQUEST_COMPLETE");
        MDC.put("endpoint", endpoint);
        MDC.put("shortCode", shortCode);
        MDC.put("totalTime", String.valueOf(totalTimeMs));
        logger.info("REQUEST_COMPLETE - {} completed in {}ms", endpoint, totalTimeMs);
        MDC.clear();
    }

    // Error Logging
    public void logError(String operation, String shortCode, Exception error) {
        MDC.put("operation", "ERROR");
        MDC.put("errorOperation", operation);
        MDC.put("shortCode", shortCode);
        logger.error("ERROR in {} for shortCode {}: {}", operation, shortCode, error.getMessage(), error);
        MDC.clear();
    }

    // Performance Metrics
    public void logPerformanceMetrics(String operation, long cacheTime, long dbTime, boolean cacheUsed) {
        MDC.put("operation", "PERFORMANCE_METRICS");
        MDC.put("cacheTime", String.valueOf(cacheTime));
        MDC.put("dbTime", String.valueOf(dbTime));
        MDC.put("cacheUsed", String.valueOf(cacheUsed));
        logger.info("PERFORMANCE - Cache: {}ms, DB: {}ms, Cache Used: {}",
                cacheTime, dbTime, cacheUsed);
        MDC.clear();
    }
}