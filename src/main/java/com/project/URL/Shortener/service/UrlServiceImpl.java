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

    public UrlServiceImpl(UrlRepo urlRepo, RedisTemplate<String, Url> redisTemplate) {
        this.urlRepo = urlRepo;
        this.redisTemplate = redisTemplate;
    }

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
        String key = "short:" + code;

        Url cachedUrl = redisTemplate.opsForValue().get(key);
        if (cachedUrl != null) {
            return cachedUrl; // ✅ Return immediately if found
        }

        Url urlFromDb = urlRepo.findAll()
                .stream()
                .filter(u -> code.equals(u.getShortCode()))
                .findFirst()
                .orElse(null);

        if (urlFromDb != null) {
            redisTemplate.opsForValue().set(key, urlFromDb, 10, TimeUnit.MINUTES);
        }

        return urlFromDb;
    }

    @Override
    public void incrementClick(String code) {
        
        // Increment click count for a URL
        Url url = getUrlByShortCode(code);
        // if (url != null) {
        //     url.setClickCount(url.getClickCount() + 1);
        //     urlRepo.save(url);
        // }
    }
}
