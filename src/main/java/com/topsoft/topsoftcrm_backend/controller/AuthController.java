package com.topsoft.topsoftcrm_backend.controller;

import com.topsoft.topsoftcrm_backend.dto.request.LoginRequest;
import com.topsoft.topsoftcrm_backend.dto.response.LoginResponse;
import com.topsoft.topsoftcrm_backend.security.JwtUtil;
import com.topsoft.topsoftcrm_backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ LOGIN
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request);

        // The token is returned from the service but we put it in the cookie,
        // NOT in the response body. LoginResponse no longer has a token field.
        String token = authService.generateTokenForUser(
                loginResponse.getId(),
                loginResponse.getUsername(),
                loginResponse.getRole().name()
        );

        setTokenCookie(response, token);
        return ResponseEntity.ok(loginResponse);
    }

    // ----------------------------------------------------------------- LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Clear the cookie by setting maxAge to 0
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    // ----------------------------------------------------------------- REFRESH
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = extractTokenFromCookie(request);

        if (token == null || !jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).build();
        }

        String id       = jwtUtil.extractId(token);
        String role     = jwtUtil.extractRole(token);
        String username = jwtUtil.extractUsername(token);

        // Verify the user is still active in the database before re-issuing
        boolean stillActive = authService.isUserActive(id, role);
        if (!stillActive) {
            return ResponseEntity.status(401).build();
        }

        String newToken = jwtUtil.generateToken(id, username, role);
        setTokenCookie(response, newToken);
        return ResponseEntity.ok().build();
    }

    // ---------------------------------------------------- UTILITY (dev only)
    @GetMapping("/hash")
    public String getHash() {
        return passwordEncoder.encode("311268");
    }

    // -------------------------------------------------------- PRIVATE HELPERS
    private void setTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)          // JS cannot read this — XSS protection
                .secure(true)            // HTTPS only
                .sameSite("Strict")      // CSRF protection
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "access_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}