package com.project.URL.Shortener.service;

import com.project.URL.Shortener.entity.Url;

import java.util.List;

public interface UrlService {
    Url saveUrl(Url url);
    List<Url> getAll();
    Url getUrlByShortCode(String code);
    Url updateUrl(String shortCode, Url updatedUrl);
    boolean deleteUrl(String shortCode);


}
