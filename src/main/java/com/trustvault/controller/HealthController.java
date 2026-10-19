package com.trustvault.controller;

import com.trustvault.dto.HealthResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * HealthController
 *
 * REST controller that exposes a health endpoint to verify that the
 * application server is up and responsive.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<HealthResponseDto> checkHealth() {
        HealthResponseDto response = new HealthResponseDto(
                "UP",
                "TrustVault",
                "0.0.1-SNAPSHOT",
                "Development",
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}
