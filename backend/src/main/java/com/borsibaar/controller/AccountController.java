package com.borsibaar.controller;

import com.borsibaar.entity.Role;
import com.borsibaar.entity.User;
import com.borsibaar.repository.RoleRepository;
import com.borsibaar.repository.UserRepository;
import com.borsibaar.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public record MeResponse(String email, String name, String role, Long organizationId, boolean needsOnboarding) {}
    public record OnboardingRequest(Long organizationId, boolean acceptTerms) {}

    @GetMapping
    public ResponseEntity<?> me(OAuth2AuthenticationToken oauth) {
        try {
            // 1) If OAuth2 session exists, return something minimal (no DB assumptions)
            if (oauth != null) {
                String email = oauth.getPrincipal().getAttribute("email");
                String name = oauth.getPrincipal().getAttribute("name");
                // If your app relies on DB user/org/role, you can look up by email here.
                return ResponseEntity.ok(new MeResponse(email, name, null, null, true));
            }

            // 2) Otherwise, fall back to your JWT/SecurityUtils-based auth (original behavior)
            User user = SecurityUtils.getCurrentUser(false);

            return ResponseEntity.ok(new MeResponse(
                    user.getEmail(),
                    user.getName(),
                    user.getRole() != null ? user.getRole().getName() : null,
                    user.getOrganizationId(),
                    user.getOrganizationId() == null
            ));
        } catch (Exception e) {
            // IMPORTANT: return 401/500 as plain responses, do not trigger redirects
            // If you're not authenticated, SecurityUtils usually throws ResponseStatusException or similar.
            // We keep it simple: treat auth failures as 401.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated."));
        }
    }

    @PostMapping("/onboarding")
    @Transactional
    public ResponseEntity<?> finish(@RequestBody OnboardingRequest req, OAuth2AuthenticationToken oauth) {
        try {
            if (req.organizationId() == null || !req.acceptTerms()) {
                return ResponseEntity.badRequest().body(Map.of("message", "organizationId and acceptTerms required"));
            }

            // Prefer JWT user (your real app logic)
            User user;
            try {
                user = SecurityUtils.getCurrentUser(false);
            } catch (Exception ignored) {
                // If you ever want OAuth2-only onboarding, you’d implement “find/create user by oauth email” here.
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Not authenticated."));
            }

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new IllegalArgumentException("Admin role ADMIN not found"));

            if (user.getOrganizationId() == null) {
                if (userRepository.findByOrganizationIdAndRole(req.organizationId(), adminRole).isEmpty()) {
                    user.setRole(adminRole);
                }
                user.setOrganizationId(req.organizationId());
                userRepository.save(user);
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to complete onboarding", "error", e.getMessage()));
        }
    }
}
