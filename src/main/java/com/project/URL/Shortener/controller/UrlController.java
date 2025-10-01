package com.project.URL.Shortener.controller;

import com.project.URL.Shortener.entity.Url;
import com.project.URL.Shortener.service.UrlService;
import com.project.URL.Shortener.service.PerformanceLogService;
import com.project.URL.Shortener.service.RedisService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/url")
public class UrlController {

    private final UrlService urlService;
    private final RedisService redisService;
    private final PerformanceLogService performanceLogService;

    @Autowired
    public UrlController(UrlService urlService,
                         RedisService redisService,
                         PerformanceLogService performanceLogService) {
        this.urlService = urlService;
        this.redisService = redisService;
        this.performanceLogService = performanceLogService;
    }

    // ---------------------- PRODUCTION ENDPOINTS ----------------------

    /**
     * Redirect (avec Redis cache + DB fallback)
     *
     * Flow :
     * 1. Check Redis cache
     * 2. If miss → Query PostgreSQL
     * 3. Save to cache for next time
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("REDIRECT", code, clientIp);

        // 1. Try cache first
        Url url = redisService.get(code);

        // 2. Cache miss → fallback to DB
        if (url == null) {
            url = urlService.getUrlByShortCode(code);

            if (url == null) {
                performanceLogService.logError("REDIRECT", code,
                        new RuntimeException("Short code not found: " + code));
                return ResponseEntity.notFound().build();
            }

            // 3. Populate cache (write-through)
            redisService.set(code, url);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        performanceLogService.logRequestComplete("REDIRECT", code, totalTime);

        return ResponseEntity.status(302)
                .location(URI.create(url.getOriginalUrl()))
                .build();
    }

    /**
     * Create URL (save to DB + cache)
     */
    @PostMapping("/create")
    public ResponseEntity<Url> createUrl(@Valid @RequestBody Url url, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("CREATE", url.getShortCode(), clientIp);

        // 1. Save to DB (source of truth)
        Url savedUrl = urlService.saveUrl(url);

        // 2. Save to cache (write-through)
        redisService.set(savedUrl.getShortCode(), savedUrl);

        performanceLogService.logRequestComplete("CREATE", url.getShortCode(), 0);
        return ResponseEntity.ok(savedUrl);
    }

    /**
     * Update URL (update DB + invalidate cache)
     */
    @PutMapping("/{code}")
    public ResponseEntity<Url> updateUrl(@PathVariable String code,
                                         @Valid @RequestBody Url updatedUrl,
                                         HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("UPDATE", code, clientIp);

        // 1. Update DB
        Url result = urlService.updateUrl(code, updatedUrl);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }


        performanceLogService.logRequestComplete("UPDATE", code, 0);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete URL (delete from DB + cache)
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String code, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("DELETE", code, clientIp);

        // 1. Delete from DB
        boolean deleted = urlService.deleteUrl(code);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }


        performanceLogService.logRequestComplete("DELETE", code, 0);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Url>> getAllUrls(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("GET_ALL", "N/A", clientIp);

        List<Url> urls = urlService.getAll();
        performanceLogService.logRequestComplete("GET_ALL", "N/A", 0);

        return ResponseEntity.ok(urls);
    }

    // ---------------------- DEBUG ENDPOINTS (remove in production) ----------------------

    @PostMapping("/debug/set")
    public String debugSetUrl(@RequestParam String code, @RequestParam String originalUrl) {
        Url url = new Url();
        url.setShortCode(code);
        url.setOriginalUrl(originalUrl);
        redisService.set(code, url);
        return "✅ Saved to cache: " + code;
    }

    @GetMapping("/debug/get")
    public String debugGetUrl(@RequestParam String code) {
        Url url = redisService.get(code);
        return (url != null) ? "✅ " + url.getOriginalUrl() : "❌ Not found in cache";
    }

    // ---------------------- Util ----------------------
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}