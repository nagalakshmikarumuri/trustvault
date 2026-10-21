package com.trustvault.controller;

import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LoginControllerTest
 *
 * Automated tests verifying the login page rendering, form login authentication flow,
 * failure redirection, logout handling, and route protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /login should return 200 OK and render the login template")
    void shouldRenderLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign In")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"username\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"password\"")));
    }

    @Test
    @DisplayName("GET /login?error should display invalid credentials notification")
    void shouldDisplayErrorMessageOnErrorParam() throws Exception {
        mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid username or password. Please try again.")));
    }

    @Test
    @DisplayName("GET /login?logout should display logout confirmation notification")
    void shouldDisplayLogoutMessageOnLogoutParam() throws Exception {
        mockMvc.perform(get("/login?logout"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You have been logged out successfully.")));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should redirect to /login")
    void unauthenticatedAccessToProtectedUrlShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/protected/documents"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("POST /login with valid credentials should successfully authenticate and redirect to /")
    void successfulLoginShouldRedirectToHome() throws Exception {
        String rawPassword = "ValidPassword123!";
        User mockUser = new User("valid_user", passwordEncoder.encode(rawPassword), Role.VIEWER, true);

        when(userRepository.findByUsername("valid_user")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "valid_user")
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /login with invalid credentials should fail authentication and redirect to /login?error")
    void failedLoginShouldRedirectToLoginError() throws Exception {
        when(userRepository.findByUsername("nonexistent_user")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "nonexistent_user")
                        .param("password", "wrong_password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DisplayName("POST /logout should invalidate session and redirect to /login?logout")
    void logoutShouldRedirectToLoginLogout() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }
}
