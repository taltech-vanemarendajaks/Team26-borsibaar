package com.borsibaar.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AccountController {

    @GetMapping("/api/account")
    public Map<String, Object> account(OAuth2AuthenticationToken auth) {
        return Map.of(
            "email", auth.getPrincipal().getAttribute("email"),
            "needsOnboarding", false // or real value if you have it
        );
    }
}
