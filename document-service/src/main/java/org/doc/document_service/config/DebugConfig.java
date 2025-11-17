package org.doc.document_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class DebugConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                    System.out.println("=== JWT DEBUG (Interceptor) ===");
                    System.out.println("JWT subject: " + jwt.getSubject());
                    System.out.println("JWT scopes:  " + jwt.getClaim("scope"));
                    System.out.println("JWT roles:   " + jwt.getClaim("roles"));
                    System.out.println("All claims:  " + jwt.getClaims());
                    System.out.println("Authorities: " + auth.getAuthorities());
                }

                return true;
            }
        });
    }
}
