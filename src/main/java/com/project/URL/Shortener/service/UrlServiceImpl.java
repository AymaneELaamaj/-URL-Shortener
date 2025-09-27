package com.project.URL.Shortener.service;

import com.project.URL.Shortener.entity.Url;
import com.project.URL.Shortener.repository.UrlRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlServiceImpl implements UrlService {

    
    private final UrlRepo urlRepo;
    private final RedisTemplate<String, Url> redisTemplate;
    private final PerformanceLogService performanceLogService;

    public UrlServiceImpl(UrlRepo urlRepo, RedisTemplate<String, Url> redisTemplate, PerformanceLogService performanceLogService) {
        this.urlRepo = urlRepo;
        this.redisTemplate = redisTemplate;
        this.performanceLogService = performanceLogService;
        
    }

    @Override
    public Url  saveUrl(Url url) {
        long dbStartTime = System.currentTimeMillis();
        Url savedUrl = urlRepo.save(url);
        long dbTime = System.currentTimeMillis() - dbStartTime;
        performanceLogService.logDatabaseSave(url.getShortCode(), url.getOriginalUrl(), dbTime);
        return savedUrl;
    }

    @Override
    public List<Url> getAll() {
        
        // Return all URLs
        return urlRepo.findAll();
    }

    @Override
    public Url getUrlByShortCode(String code) {
        long startTime = System.currentTimeMillis();
        long cacheTime = 0;
        long dbTime = 0;
        boolean cacheUsed = false;

        try {
            // 1. Log request start
            performanceLogService.logRequestStart("GET_URL", code, "");

            String redisKey = "short:" + code;

            // 2. Try Redis cache first (with timing)
            long cacheStartTime = System.currentTimeMillis();
            Url cachedUrl = redisTemplate.opsForValue().get(redisKey);
            cacheTime = System.currentTimeMillis() - cacheStartTime;

            if (cachedUrl != null) {
                // 3. Cache HIT - return immediately
                performanceLogService.logCacheHit(code, "GET_URL");
                cacheUsed = true;

                // Increment click count in background (doesn't block response)
                incrementClickAsync(code);

                // Log performance and complete
                long totalTime = System.currentTimeMillis() - startTime;
                performanceLogService.logPerformanceMetrics("GET_URL", cacheTime, dbTime, cacheUsed);
                performanceLogService.logRequestComplete("GET_URL", code, totalTime);

                return cachedUrl;
            }

            // 4. Cache MISS - query database
            performanceLogService.logCacheMiss(code, "GET_URL");

            long dbStartTime = System.currentTimeMillis();
            Optional<Url> urlOptional = urlRepo.findByShortCode(code);
            dbTime = System.currentTimeMillis() - dbStartTime;

            if (urlOptional.isPresent()) {
                // 5. Found in database
                Url urlFromDb = urlOptional.get();

                performanceLogService.logDatabaseQuery(code, "SELECT_BY_CODE", dbTime);

                // Store in cache for next time
                long cacheStoreStart = System.currentTimeMillis();
                redisTemplate.opsForValue().set(redisKey, urlFromDb, 24, TimeUnit.HOURS);
                long cacheStoreTime = System.currentTimeMillis() - cacheStoreStart;

                performanceLogService.logCacheStore(code, urlFromDb.getOriginalUrl());

                // Increment click count in background
                incrementClickAsync(code);

                // Log performance metrics
                long totalTime = System.currentTimeMillis() - startTime;
                performanceLogService.logPerformanceMetrics("GET_URL", cacheTime + cacheStoreTime, dbTime, cacheUsed);
                performanceLogService.logRequestComplete("GET_URL", code, totalTime);

                return urlFromDb;
            }

            // 6. Not found anywhere
            performanceLogService.logDatabaseQuery(code, "SELECT_NOT_FOUND", dbTime);

            long totalTime = System.currentTimeMillis() - startTime;
            performanceLogService.logRequestComplete("GET_URL", code, totalTime);

            return null;

        } catch (Exception e) {
            // 7. Handle any errors
            performanceLogService.logError("GET_URL", code, e);
            throw e;
        }
    }


    /**
     * Async click increment - separates click tracking from read path
     * This prevents click updates from slowing down redirects
     */
    private void incrementClickAsync(String code) {
        // Run in separate thread to not block the redirect response
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // 1. Find the URL by short code
                Optional<Url> urlOptional = urlRepo.findByShortCode(code);

                if (urlOptional.isPresent()) {
                    // 2. Increment click count
                    Url url = urlOptional.get();
                    url.setClickCount(url.getClickCount() + 1);

                    // 3. Save back to database
                    urlRepo.save(url);

                    // 4. Log the operation timing
                    long executionTime = System.currentTimeMillis() - startTime;
                    performanceLogService.logDatabaseQuery(code, "INCREMENT_CLICKS", executionTime);

                    // 5. Update cache with new click count (keep cache fresh)
                    String redisKey = "short:" + code;
                    redisTemplate.opsForValue().set(redisKey, url, 24, TimeUnit.HOURS);

                    performanceLogService.logCacheStore(code, url.getOriginalUrl());

                } else {
                    // URL not found - log error
                    performanceLogService.logError("INCREMENT_CLICKS", code,
                            new RuntimeException("URL not found for click increment"));
                }

            } catch (Exception e) {
                // Log any errors that occur during click increment
                performanceLogService.logError("INCREMENT_CLICKS", code, e);
            }
        }).start(); // 🚨 Start the thread immediately
    }
}
