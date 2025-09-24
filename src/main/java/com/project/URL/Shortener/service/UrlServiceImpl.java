package com.project.URL.Shortener.service;

import com.project.URL.Shortener.entity.Url;
import com.project.URL.Shortener.repository.UrlRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UrlServiceImpl implements UrlService {

    @Autowired
    private UrlRepo urlRepo;

    @Override
    public Url saveUrl(Url url) {
        // Save the URL to DB
        return urlRepo.save(url);
    }

    @Override
    public List<Url> getAll() {
        // Return all URLs
        return urlRepo.findAll();
    }

    @Override
    public Url getUrlByShortCode(String code) {
        // Find URL by short code
        Optional<Url> optionalUrl = urlRepo.findAll().stream()
                .filter(u -> u.getShortCode().equals(code))
                .findFirst();
        return optionalUrl.orElse(null);
    }

    @Override
    public void incrementClick(String code) {
        // Increment click count for a URL
        Url url = getUrlByShortCode(code);
        if (url != null) {
            url.setClickCount(url.getClickCount() + 1);
            urlRepo.save(url);
        }
    }
}
