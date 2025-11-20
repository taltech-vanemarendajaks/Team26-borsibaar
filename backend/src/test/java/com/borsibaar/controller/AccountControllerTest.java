package com.borsibaar.controller;

import com.borsibaar.entity.Role;
import com.borsibaar.entity.User;
import com.borsibaar.repository.RoleRepository;
import com.borsibaar.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMe_WithNoOrganization_ReturnsNeedsOnboarding() throws Exception {
        User user = userWithOrgAndRole(null, "USER");
        setAuth(user);

        mockMvc.perform(get("/api/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.name").value(user.getName()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.organizationId").doesNotExist())
                .andExpect(jsonPath("$.needsOnboarding").value(true));

        verify(userRepository, never()).save(any());
    }

    @Test
    void onboarding_WithValidPayload_SetsOrganizationAndReturns204() throws Exception {
        User user = userWithOrgAndRole(null, "USER");
        setAuth(user);

        Role adminRole = Role.builder().id(1L).name("ADMIN").build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));

        String payload = "{\"organizationId\":1,\"acceptTerms\":true}";

        mockMvc.perform(post("/api/account/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void onboarding_WithInvalidPayload_ReturnsBadRequest() throws Exception {
        User user = userWithOrgAndRole(null, "USER");
        setAuth(user);

        String payload = "{\"organizationId\":1,\"acceptTerms\":false}"; // terms not accepted

        mockMvc.perform(post("/api/account/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any(User.class));
    }

    private static User userWithOrgAndRole(Long orgId, String roleName) {
        Role role = Role.builder().id(1L).name(roleName).build();
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .name("Test User")
                .organizationId(orgId)
                .role(role)
                .build();
    }

    private static void setAuth(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
