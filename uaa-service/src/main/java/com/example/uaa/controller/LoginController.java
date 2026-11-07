package com.example.uaa.controller;

import com.example.common.JwtUtil;
import com.example.uaa.entity.User;
import com.example.uaa.repository.UserRepository;
import com.example.uaa.service.LdapAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final LdapAuthService ldapAuthService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${github.client-id:}")
    private String githubClientId;

    @Value("${github.client-secret:}")
    private String githubClientSecret;

    public LoginController(UserRepository userRepository,
                           LdapAuthService ldapAuthService,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.ldapAuthService = ldapAuthService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(@RequestParam(value = "token", required = false) String token,
                            @RequestParam(value = "username", required = false) String username,
                            @RequestParam(value = "role", required = false) String role,
                            Model model) {
        model.addAttribute("token", token);
        model.addAttribute("username", username);
        model.addAttribute("role", role);
        return "login";
    }

    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> apiLogin(@RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "db");
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username and password cannot be empty"));
        }

        String matchedRole = null;

        if ("ldap".equalsIgnoreCase(type) || username.startsWith("ldap_")) {
            matchedRole = ldapAuthService.authenticate(username, password);
        } else {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordEncoder.matches(password, user.getPassword()) || password.equals(user.getPassword())) {
                    matchedRole = user.getRole();
                }
            } else {
                matchedRole = ldapAuthService.authenticate(username, password);
            }
        }

        if (matchedRole == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication failed: Invalid username or password"));
        }

        String token = jwtUtil.generateToken(username, matchedRole);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "access_token", token,
                "token_type", "bearer",
                "username", username,
                "role", matchedRole
        ));
    }

    @GetMapping("/oauth2/github/login")
    public RedirectView githubLogin(@RequestParam(value = "simulate", defaultValue = "false") boolean simulate) {
        if (!simulate && githubClientId != null && !githubClientId.isBlank() && !githubClientId.equals("YOUR_GITHUB_CLIENT_ID")) {
            // Real GitHub OAuth2 redirect
            String redirectUrl = "https://github.com/login/oauth/authorize?client_id=" + githubClientId + "&scope=user:email";
            return new RedirectView(redirectUrl);
        } else {
            // Simulated GitHub login: assigns ROLE_EDITOR per requirements
            String mockUsername = "github_user";
            String role = "ROLE_EDITOR";
            String token = jwtUtil.generateToken(mockUsername, role);
            return new RedirectView("/login?token=" + token + "&username=" + mockUsername + "&role=" + role);
        }
    }

    @GetMapping("/oauth2/github/callback")
    public RedirectView githubCallback(@RequestParam(value = "code", required = false) String code) {
        // According to requirements: GITHUB grants EDITOR role upon login
        String username = "github_user";
        String role = "ROLE_EDITOR";
        String token = jwtUtil.generateToken(username, role);
        return new RedirectView("/login?token=" + token + "&username=" + username + "&role=" + role);
    }
}
