package com.project.URL.Shortener.config;

import com.project.URL.Shortener.repository.UrlRepo;
import com.project.URL.Shortener.service.PerformanceLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SyncJob {
    @Autowired
    private UrlRepo urlRepo;
    @Autowired
    private RedisTemplate<String, Long> redisTemplate;
    @Autowired
    private PerformanceLogService performanceLogService;

    @Scheduled(fixedRate = 60000) // toutes les 1 minute
    public void syncClicksToDatabase() {
        long startTime = System.currentTimeMillis();
        int processedKeys = 0;
        int totalClicksProcessed = 0;

        try {
            performanceLogService.logRequestStart("SYNC_BATCH", "N/A", "SCHEDULER");

            Set<String> keys = redisTemplate.keys("click:*");
            if (keys == null || keys.isEmpty()) {
                performanceLogService.logRequestComplete("SYNC_BATCH", "N/A",
                        System.currentTimeMillis() - startTime);
                return;
            }

            for (String key : keys) {
                try {
                    Long count = redisTemplate.opsForValue().get(key);
                    if (count == null || count <= 0) continue;

                    String code = key.substring(6); // enlever "click:" prefix

                    long dbStartTime = System.currentTimeMillis();
                    urlRepo.findByShortCode(code).ifPresent(url -> {
                        url.setClickCount(url.getClickCount() + count);
                        urlRepo.save(url);
                        performanceLogService.logDatabaseQuery(code, "BATCH_UPDATE",
                                System.currentTimeMillis() - dbStartTime);
                    });

                    redisTemplate.delete(key); // reset le compteur Redis
                    processedKeys++;
                    totalClicksProcessed += count.intValue();

                } catch (Exception e) {
                    performanceLogService.logError("SYNC_BATCH_ITEM",
                            key.substring(6), e);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            performanceLogService.logRequestComplete("SYNC_BATCH",
                    "Processed " + processedKeys + " keys, " + totalClicksProcessed + " clicks",
                    totalTime);

        } catch (Exception e) {
            performanceLogService.logError("SYNC_BATCH", "N/A", e);
        }
    }
}