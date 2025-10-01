package com.project.URL.Shortener.service;

import com.project.URL.Shortener.entity.Url;
import com.project.URL.Shortener.repository.UrlRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlServiceImpl implements UrlService {


    private final UrlRepo urlRepo;
    private final PerformanceLogService performanceLogService;
    private final RedisTemplate<String, Long> clickRedisTemplate;
    private  final RedisService redisService;
    public UrlServiceImpl(RedisService redisService,UrlRepo urlRepo,@Qualifier("clickCounterRedisTemplate") RedisTemplate<String, Long> clickRedisTemplate
, PerformanceLogService performanceLogService) {
        this.urlRepo = urlRepo;
        this.performanceLogService = performanceLogService;
        this.clickRedisTemplate=clickRedisTemplate;
        this.redisService=redisService;

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
    // SOLUTION : Modifier UrlServiceImpl pour utiliser RedisService

    public Url getUrlByShortCode(String code) {
        long startTime = System.currentTimeMillis();

        // ✅ UTILISER RedisService (sharding) au lieu de redisTemplate direct
        Url cachedUrl = redisService.get(code);  // ← CHANGEMENT CRITIQUE

        if (cachedUrl != null) {
            // Cache HIT - return
            incrementClickAsync(code);
            return cachedUrl;
        }

        // Cache MISS - query database
        Optional<Url> urlOptional = urlRepo.findByShortCode(code);

        if (urlOptional.isPresent()) {
            Url urlFromDb = urlOptional.get();

            // ✅ UTILISER RedisService pour stocker
            redisService.set(code, urlFromDb);  // ← CHANGEMENT CRITIQUE

            incrementClickAsync(code);
            return urlFromDb;
        }

        return null;
    }


    /**
     * Async click increment - separates click tracking from read path
     * This prevents click updates from slowing down redirects
     */

    @Async
    public void incrementClickAsync(String code) {
        try {
            String redisKey = "click:" + code;
            clickRedisTemplate.opsForValue().increment(redisKey, 1);
            clickRedisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            performanceLogService.logError("INCREMENT_CLICKS", code, e);
        }
    }


    // Ajouter ces méthodes dans UrlServiceImpl:

    @Override
    public Url updateUrl(String shortCode, Url updatedUrl) {
        long startTime = System.currentTimeMillis();

        try {
            performanceLogService.logRequestStart("UPDATE_URL", shortCode, "");

            // 1. Vérifier que l'URL existe
            Optional<Url> existingUrlOpt = urlRepo.findByShortCode(shortCode);
            if (!existingUrlOpt.isPresent()) {
                performanceLogService.logError("UPDATE_URL", shortCode,
                        new RuntimeException("URL not found for update"));
                return null;
            }

            // 2. Update en base de données
            long dbStartTime = System.currentTimeMillis();
            Url existingUrl = existingUrlOpt.get();
            existingUrl.setOriginalUrl(updatedUrl.getOriginalUrl());
            // Garder les autres champs (clickCount, createdAt, etc.)

            Url savedUrl = urlRepo.save(existingUrl);
            long dbTime = System.currentTimeMillis() - dbStartTime;

            performanceLogService.logDatabaseQuery(shortCode, "UPDATE", dbTime);

            // 3. 🚨 CACHE INVALIDATION - Supprimer de Redis
            long cacheStartTime = System.currentTimeMillis();
            redisService.delete(shortCode);
            long cacheTime = System.currentTimeMillis() - cacheStartTime;



            // 4. Log completion
            long totalTime = System.currentTimeMillis() - startTime;
            performanceLogService.logRequestComplete("UPDATE_URL", shortCode, totalTime);

            return savedUrl;

        } catch (Exception e) {
            performanceLogService.logError("UPDATE_URL", shortCode, e);
            throw e;
        }
    }

    @Override
    public boolean deleteUrl(String shortCode) {
        long startTime = System.currentTimeMillis();

        try {
            performanceLogService.logRequestStart("DELETE_URL", shortCode, "");

            // 1. Vérifier que l'URL existe
            Optional<Url> existingUrlOpt = urlRepo.findByShortCode(shortCode);
            if (!existingUrlOpt.isPresent()) {
                performanceLogService.logError("DELETE_URL", shortCode,
                        new RuntimeException("URL not found for deletion"));
                return false;
            }

            // 2. Delete de la base de données
            long dbStartTime = System.currentTimeMillis();
            urlRepo.deleteById(existingUrlOpt.get().getId());
            long dbTime = System.currentTimeMillis() - dbStartTime;

            performanceLogService.logDatabaseQuery(shortCode, "DELETE", dbTime);

            // 3. 🚨 CACHE INVALIDATION - Supprimer de Redis
            long cacheStartTime = System.currentTimeMillis();
            redisService.delete(shortCode);
            long cacheTime = System.currentTimeMillis() - cacheStartTime;



            // 4. Aussi supprimer les compteurs de clics Redis
            String clickKey = "click:" + shortCode;
            clickRedisTemplate.delete(clickKey);

            // 5. Log completion
            long totalTime = System.currentTimeMillis() - startTime;
            performanceLogService.logRequestComplete("DELETE_URL", shortCode, totalTime);

            return true;

        } catch (Exception e) {
            performanceLogService.logError("DELETE_URL", shortCode, e);
            throw e;
        }
    }


}
