package com.example.orders.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class OrdersController {

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
            "service", "orders-service",
            "timestamp", Instant.now().toString(),
            "instanceId", getHostName()
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(Map.of(
            "service", "orders-service",
            "instanceId", getHostName(),
            "totalOrders", 152,
            "totalRevenue", 24890.75,
            "currency", "USD",
            "recentOrders", List.of(
                Map.of("id", "ORD-1001", "customer", "Alice", "amount", 129.99, "status", "COMPLETED"),
                Map.of("id", "ORD-1002", "customer", "Bob", "amount", 54.50, "status", "SHIPPED"),
                Map.of("id", "ORD-1003", "customer", "Charlie", "amount", 310.00, "status", "PROCESSING")
            )
        ));
    }
}
