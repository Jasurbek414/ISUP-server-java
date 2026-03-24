package com.isup.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Login endpoint — accepts username + password, returns Bearer token.
 * No authentication required on this endpoint.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${isup.admin.secret:changeme123}")
    private String adminSecret;

    @Value("${isup.admin.username:admin}")
    private String adminUsername;

    @Value("${isup.admin.password:admin123}")
    private String adminPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.username() == null || req.password() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username va parol kiritilishi shart"));
        }
        if (adminUsername.equals(req.username()) && adminPassword.equals(req.password())) {
            return ResponseEntity.ok(Map.of(
                    "token", adminSecret,
                    "username", adminUsername
            ));
        }
        return ResponseEntity.status(401)
                .body(Map.of("error", "Login yoki parol noto'g'ri"));
    }

    record LoginRequest(String username, String password) {}
}
