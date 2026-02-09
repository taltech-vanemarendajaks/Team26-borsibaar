package com.borsibaar.config;

import com.borsibaar.entity.User;
import com.borsibaar.repository.UserRepository;
import com.borsibaar.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Paths that must NEVER be touched by JWT authentication.
     * OAuth relies on sessions and redirects — JWT here breaks it.
     */
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/oauth2/",
            "/login/oauth2/",
            "/auth/login/success",
            "/error"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract JWT token from cookie
        String token = extractJwtFromCookie(request);

        // If no token, continue without authentication
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Parse and validate JWT token
            Claims claims = jwtService.parseToken(token);
            String email = claims.getSubject();

            if (email != null) {
                Optional<User> userOptional = userRepository.findByEmailWithRole(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    user.getRole() != null
                                            ? Collections.singletonList(
                                                    new SimpleGrantedAuthority(
                                                            "ROLE_" + user.getRole().getName()))
                                            : Collections.emptyList()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("JWT authentication set for user: {}", email);
                } else {
                    logger.warn("User not found for email: {}", email);
                }
            }
        } catch (Exception e) {
            // Invalid token → no auth, let Spring handle 401/403
            logger.warn("JWT token validation failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the "jwt" cookie.
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("jwt".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
