package com.project.URL.Shortener.controller;

import com.project.URL.Shortener.entity.Url;
import com.project.URL.Shortener.service.UrlService;
import com.project.URL.Shortener.service.PerformanceLogService;
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

    @Autowired
    private UrlService urlService;

    @Autowired
    private PerformanceLogService performanceLogService;

    @PostMapping("/create")
    public ResponseEntity<Url> createUrl(@Valid @RequestBody Url url, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("CREATE", url.getShortCode(), clientIp);

        Url savedUrl = urlService.saveUrl(url);
        return ResponseEntity.ok(savedUrl);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Url>> getAllUrls(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("GET_ALL", "N/A", clientIp);

        return ResponseEntity.ok(urlService.getAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        String clientIp = getClientIp(request);

        performanceLogService.logRequestStart("REDIRECT", code, clientIp);

        Url url = urlService.getUrlByShortCode(code);

        if (url == null) {
            performanceLogService.logError("REDIRECT", code,
                    new RuntimeException("Short code not found: " + code));
            return ResponseEntity.notFound().build();
        }


        long totalTime = System.currentTimeMillis() - startTime;
        performanceLogService.logRequestComplete("REDIRECT", code, totalTime);

        return ResponseEntity.status(302)
                .location(URI.create(url.getOriginalUrl()))
                .build();
    }


    // Ajouter ces méthodes dans UrlController:

    @PutMapping("/{code}")
    public ResponseEntity<Url> updateUrl(@PathVariable String code,
                                         @Valid @RequestBody Url updatedUrl,
                                         HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("UPDATE", code, clientIp);

        Url result = urlService.updateUrl(code, updatedUrl);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteUrl(@PathVariable String code,
                                          HttpServletRequest request) {
        String clientIp = getClientIp(request);
        performanceLogService.logRequestStart("DELETE", code, clientIp);

        boolean deleted = urlService.deleteUrl(code);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

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