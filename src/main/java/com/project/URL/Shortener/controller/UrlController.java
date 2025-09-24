package com.project.URL.Shortener.controller;

import com.project.URL.Shortener.entity.Url;
import com.project.URL.Shortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/url")
public class UrlController {

    @Autowired
    private UrlService urlService;

    @PostMapping("/create")
    public ResponseEntity<Url> createUrl(@Valid @RequestBody Url url) {
        Url savedUrl = urlService.saveUrl(url);
        return ResponseEntity.ok(savedUrl);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Url>> getAllUrls() {
        return ResponseEntity.ok(urlService.getAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        Url url = urlService.getUrlByShortCode(code);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }

        urlService.incrementClick(code); // increment clicks

        return ResponseEntity.status(302).location(URI.create(url.getOriginalUrl())).build();
    }
}
