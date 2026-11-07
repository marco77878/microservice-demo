package com.example.uaa.controller;

import com.example.common.JwtUtil;
import com.example.uaa.entity.User;
import com.example.uaa.repository.UserRepository;
import com.example.uaa.service.LdapAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class TokenController {

    private final UserRepository userRepository;
    private final LdapAuthService ldapAuthService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public TokenController(UserRepository userRepository,
                           LdapAuthService ldapAuthService,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.ldapAuthService = ldapAuthService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value = {"/token", "/uaa/token"}, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> tokenForm(@RequestParam MultiValueMap<String, String> paramMap) {
        String username = paramMap.getFirst("username");
        String password = paramMap.getFirst("password");
        String grantType = paramMap.getFirst("grant_type");

        return authenticateAndGenerateToken(username, password, grantType);
    }

    @PostMapping(value = {"/token", "/uaa/token"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> tokenJson(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String grantType = body.getOrDefault("grant_type", "password");

        return authenticateAndGenerateToken(username, password, grantType);
    }

    private ResponseEntity<?> authenticateAndGenerateToken(String username, String password, String grantType) {
        if (username == null || password == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "invalid_request");
            error.put("error_description", "username and password parameters are required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String matchedRole = null;

        // 1. Check LDAP if username starts with ldap_
        if (username.startsWith("ldap_")) {
            matchedRole = ldapAuthService.authenticate(username, password);
        } else {
            // 2. Check Database
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordEncoder.matches(password, user.getPassword()) || password.equals(user.getPassword())) {
                    matchedRole = user.getRole();
                }
            } else {
                // Also check LDAP as fallback
                matchedRole = ldapAuthService.authenticate(username, password);
            }
        }

        if (matchedRole == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "invalid_grant");
            error.put("error_description", "Bad credentials: invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Generate JWT
        String token = jwtUtil.generateToken(username, matchedRole);

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "bearer");
        response.put("expires_in", 86400);
        response.put("scope", "read write");
        response.put("role", matchedRole);
        response.put("username", username);

        return ResponseEntity.ok(response);
    }
}
