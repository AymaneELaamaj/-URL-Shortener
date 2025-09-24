package com.project.URL.Shortener.repository;

import com.project.URL.Shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlRepo extends JpaRepository<Url,Long> {
}
