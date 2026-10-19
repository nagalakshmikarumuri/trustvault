package com.trustvault.dto;

import java.time.LocalDateTime;

/**
 * HealthResponseDto
 *
 * Data Transfer Object representing the payload returned by the health check API.
 */
public class HealthResponseDto {

    private String status;
    private String application;
    private String version;
    private String environment;
    private LocalDateTime timestamp;

    public HealthResponseDto() {
    }

    public HealthResponseDto(String status, String application, String version, String environment, LocalDateTime timestamp) {
        this.status = status;
        this.application = application;
        this.version = version;
        this.environment = environment;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
