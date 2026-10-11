package com.example.catalog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class CatalogController {

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "catalog-service",
            "timestamp", Instant.now().toString(),
            "instanceId", getHostName()
        ));
    }

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> items() {
        return ResponseEntity.ok(Map.of(
            "service", "catalog-service",
            "instanceId", getHostName(),
            "count", 4,
            "items", List.of(
                Map.of("id", "ITEM-101", "name", "Mechanical Keyboard", "category", "Electronics", "price", 89.99, "inStock", true),
                Map.of("id", "ITEM-102", "name", "Ergonomic Mouse", "category", "Electronics", "price", 49.50, "inStock", true),
                Map.of("id", "ITEM-103", "name", "4K Ultra-HD Monitor", "category", "Displays", "price", 379.00, "inStock", false),
                Map.of("id", "ITEM-104", "name", "Noise-Cancelling Headphones", "category", "Audio", "price", 199.95, "inStock", true)
            )
        ));
    }
}
