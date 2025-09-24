package com.project.URL.Shortener.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Original URL cannot be blank")
    @Pattern(
            regexp = "^(https?://).+",
            message = "URL must start with http:// or https://"
    )
    private String originalUrl;
    @Column(unique = true)
    @Size(min = 6, max = 8, message = "Short code must be 6-8 characters")

    private String shortCode;
    private Long clickCount;
    private Timestamp createdAt;
    @PrePersist
    protected void onCreate() {
        if (clickCount == null) {
            clickCount = 0L; // Initialize clickCount
        }
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis()); // Initialize createdAt
        }
    }


}
