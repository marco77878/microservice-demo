package com.example.product.security;

import com.example.common.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String username = null;
        String role = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                username = jwtUtil.extractUsername(token);
                role = jwtUtil.extractRole(token);
            }
        }

        // Fallback to headers forwarded by Gateway if present
        if (username == null) {
            username = request.getHeader("X-User-Name");
            role = request.getHeader("X-User-Roles");
        }

        if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role));

            // Expand Role Hierarchy:
            // PRODUCT_ADMIN has EDITOR and USER
            // EDITOR has USER
            if ("ROLE_PRODUCT_ADMIN".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_EDITOR"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            } else if ("ROLE_EDITOR".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
