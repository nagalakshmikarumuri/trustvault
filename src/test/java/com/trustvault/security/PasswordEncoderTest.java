package com.trustvault.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PasswordEncoderTest
 *
 * Verifies that the PasswordEncoder bean is configured in the Spring application context
 * and performs BCrypt hashing and verification as expected.
 */
@SpringBootTest
class PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("PasswordEncoder bean should be successfully injected from the application context")
    void passwordEncoderBeanShouldExist() {
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    @DisplayName("BCryptPasswordEncoder should produce a secure hash distinct from raw input and verify matches correctly")
    void shouldEncodeAndMatchHarmlessSampleValue() {
        String harmlessSample = "sample-test-value-12345";

        // Verify encoding produces a non-null, non-empty hash distinct from the original value
        String encodedHash = passwordEncoder.encode(harmlessSample);
        assertThat(encodedHash)
                .isNotNull()
                .isNotBlank()
                .isNotEqualTo(harmlessSample)
                .startsWith("$2");

        // Verify the encoder successfully matches the original sample value against its hash
        assertThat(passwordEncoder.matches(harmlessSample, encodedHash)).isTrue();

        // Verify that an incorrect value fails matching
        assertThat(passwordEncoder.matches("different-harmless-value", encodedHash)).isFalse();
    }
}
