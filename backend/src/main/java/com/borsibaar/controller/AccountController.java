package com.borsibaar.controller;

import com.borsibaar.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login/success")
    public void success(HttpServletResponse response, OAuth2AuthenticationToken auth) throws IOException {
        var result = authService.processOAuthLogin(auth);

        // Force SameSite=None; Secure so cookies work properly with OAuth redirects and modern browsers
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", result.dto().token())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        String redirect = result.needsOnboarding() ? "/onboarding" : "/dashboard";
        response.sendRedirect(frontendUrl + redirect);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Invalidate the server-side session (removes OAuth2 authentication)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        // Expire JWT cookie (match attributes so browser actually removes it)
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        return ResponseEntity.ok().body(new LogoutResponse("Logged out successfully"));
    }

    private record LogoutResponse(String message) {}
}
