package com.borsibaar.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        // Force Google account chooser every login
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        OAuth2AuthorizationRequestResolver customResolver =
                new OAuth2AuthorizationRequestResolver() {
                    @Override
                    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                        OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
                        if (req == null) return null;
                        return OAuth2AuthorizationRequest.from(req)
                                .additionalParameters(p -> p.put("prompt", "select_account"))
                                .build();
                    }

                    @Override
                    public OAuth2AuthorizationRequest resolve(
                            HttpServletRequest request,
                            String clientRegistrationId
                    ) {
                        OAuth2AuthorizationRequest req =
                                defaultResolver.resolve(request, clientRegistrationId);
                        if (req == null) return null;
                        return OAuth2AuthorizationRequest.from(req)
                                .additionalParameters(p -> p.put("prompt", "select_account"))
                                .build();
                    }
                };

        return http
                // CSRF disabled for API + OAuth callback simplicity
                .csrf(csrf -> csrf.disable())

                // CORS must be enabled BEFORE security filters
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // =====================================================================
                // ✅ HIGHLIGHTED ADDITION #1: "API MUST NOT REDIRECT" (return 401)
                // This prevents login loops where API requests get 302->oauth2->... forever.
                // =====================================================================
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new OrRequestMatcher(
                                        new AntPathRequestMatcher("/api/**"),
                                        new AntPathRequestMatcher("/v3/api-docs/**"),
                                        new AntPathRequestMatcher("/swagger-ui/**"),
                                        new AntPathRequestMatcher("/swagger-ui.html"),
                                        new AntPathRequestMatcher("/actuator/**")
                                )
                        )
                )
                // =====================================================================

                // JWT filter applies ONLY to API requests (shouldNotFilter handles exclusions)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // OAuth uses sessions, API uses JWT → IF_REQUIRED is correct
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // OAuth + public endpoints
                        .requestMatchers(
                                "/",
                                "/error",
                                "/oauth2/**",
                                "/login/oauth2/code/**",
                                "/auth/login/success"
                        ).permitAll()

                        // =================================================================
                        // ✅ HIGHLIGHTED ADDITION #2 (OPTIONAL but recommended):
                        // If you want docs/actuator publicly visible, keep permitAll here.
                        // If you *don’t* want them public, remove these lines.
                        // (Even if protected, they will return 401 now—NOT redirect.)
                        // =================================================================
                        // .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // .requestMatchers("/actuator/**").permitAll()
                        // =================================================================

                        // Public API endpoints (GET + HEAD)
                        .requestMatchers(HttpMethod.GET,  "/api/organizations/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/organizations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/organizations").permitAll()

                        .requestMatchers(HttpMethod.GET,  "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/categories/**").permitAll()

                        .requestMatchers(HttpMethod.GET,  "/api/inventory/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/inventory/**").permitAll()

                        // Admin-only
                        .requestMatchers(HttpMethod.PUT, "/api/organizations/**")
                        .hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        // Backend endpoint that sets JWT / cookie
                        .defaultSuccessUrl("/auth/login/success", true)
                        .authorizationEndpoint(auth ->
                                auth.authorizationRequestResolver(customResolver)
                        )
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
