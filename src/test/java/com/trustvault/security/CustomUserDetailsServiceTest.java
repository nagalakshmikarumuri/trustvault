package com.trustvault.security;

import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CustomUserDetailsServiceTest
 *
 * Focused unit tests for CustomUserDetailsService using a mocked UserRepository.
 * Verifies mapping of domain User entities to Spring Security UserDetails and
 * exception handling when a user is not found.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Should successfully load an existing user and map all fields correctly to UserDetails")
    void shouldLoadExistingUserAndMapCorrectly() {
        User user = new User("admin_user", "$2a$10$sampleHashedPasswordString", Role.ADMIN, true);
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin_user");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("admin_user");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$sampleHashedPasswordString");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");

        verify(userRepository).findByUsername("admin_user");
    }

    @Test
    @DisplayName("Should correctly reflect disabled account status and OWNER role")
    void shouldMapDisabledStatusAndOwnerRoleCorrectly() {
        User user = new User("doc_owner", "$2a$10$anotherHashedPasswordString", Role.OWNER, false);
        when(userRepository.findByUsername("doc_owner")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("doc_owner");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("doc_owner");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$anotherHashedPasswordString");
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OWNER");

        verify(userRepository).findByUsername("doc_owner");
    }

    @Test
    @DisplayName("Should correctly map VIEWER role")
    void shouldMapViewerRoleCorrectly() {
        User user = new User("viewer_user", "$2a$10$viewerHash", Role.VIEWER, true);
        when(userRepository.findByUsername("viewer_user")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("viewer_user");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VIEWER");

        verify(userRepository).findByUsername("viewer_user");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user is not found in UserRepository")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("missing_user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing_user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing_user");

        verify(userRepository).findByUsername("missing_user");
    }
}
