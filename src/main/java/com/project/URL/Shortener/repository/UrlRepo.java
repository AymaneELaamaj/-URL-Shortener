package com.project.URL.Shortener.repository;

import com.project.URL.Shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UrlRepo extends JpaRepository<Url,Long> {
    Optional<Url> findByShortCode(String shortCode);

}
