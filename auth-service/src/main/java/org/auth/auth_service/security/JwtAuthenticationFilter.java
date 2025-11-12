package org.auth.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.auth.auth_service.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // no token -> just continue (some endpoints permitted)
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            JWTClaimsSet claims = jwtUtil.parseAndVerify(token);
            // Claims claims = jws.getBody();
            String subject = claims.getSubject();

            // roles can be stored as List<String> or CSV. Handle both.
            List<String> roles = new ArrayList<>();
            Object rolesObj = claims.getStringListClaim("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) rolesObj;
                roles = list.stream().map(Object::toString).collect(Collectors.toList());
            } else if (rolesObj instanceof String) {
                String s = (String) rolesObj;
                roles = Arrays.stream(s.split(",")).map(String::trim).filter(r -> !r.isEmpty())
                        .collect(Collectors.toList());
            }

            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(subject, null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            // Token invalid/expired -> respond 401 JSON and do not continue filter chain
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String body = "{\"error\":\"invalid_token\",\"message\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
            response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
    }
}