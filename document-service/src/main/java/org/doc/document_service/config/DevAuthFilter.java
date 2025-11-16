package org.doc.document_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Small filter to create an Authentication when the X-DEV-USER header is present.
 * Usage (curl): -H "X-DEV-USER: alice"
 *
 * In mock mode it also grants SCOPE_doc:create authority so POST /documents is allowed.
 */
@Component
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-DEV-USER";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String devUser = request.getHeader(HEADER);
        if (!StringUtils.hasText(devUser)) {
            // no header — don't set auth; downstream may reject if resource server not enabled
            filterChain.doFilter(request, response);
            return;
        }

        // Create a very small Authentication object with SCOPE_doc:create for quick testing.
        // Authorities must be prefixed with "SCOPE_" if your @PreAuthorize uses 'hasAuthority("SCOPE_doc:create")'
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("SCOPE_doc:create"),
                new SimpleGrantedAuthority("ROLE_USER")
        );

        AbstractAuthenticationToken auth = new AbstractAuthenticationToken(authorities) {
            private final String principal = devUser;

            @Override
            public Object getCredentials() { return ""; }

            @Override
            public Object getPrincipal() { return principal; }
        };
        auth.setAuthenticated(true);

        // set details (optional)
        auth.setDetails(request.getRemoteAddr());

        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // clear after request to avoid leakage
            SecurityContextHolder.clearContext();
        }
    }
}
