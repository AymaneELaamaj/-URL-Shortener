package com.project.URL.Shortener.service;

import com.project.URL.Shortener.entity.Url;

import java.util.List;

public interface UrlService {
    Url saveUrl(Url url);
    List<Url> getAll();
    Url getUrlByShortCode(String code);
    void incrementClick(String code);

}
