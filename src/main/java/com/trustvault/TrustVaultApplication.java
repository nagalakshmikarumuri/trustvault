package com.trustvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TrustVaultApplication
 *
 * The main entry point for the TrustVault Spring Boot application.
 *
 * Annotations:
 * - @SpringBootApplication: Combines @Configuration, @EnableAutoConfiguration,
 *   and @ComponentScan to scan and configure all Spring components inside the
 *   base package "com.trustvault".
 */
@SpringBootApplication
public class TrustVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrustVaultApplication.class, args);
    }
}
