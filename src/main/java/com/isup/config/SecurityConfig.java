package com.isup.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${isup.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${isup.admin.secret:changeme123}")
    private String adminSecret;

    // Simple in-memory rate limiter: ip -> (window_start_ms, count)
    private final ConcurrentHashMap<String, long[]> rateLimitMap = new ConcurrentHashMap<>();
    private static final int    RATE_LIMIT_MAX      = 100;
    private static final long   RATE_LIMIT_WINDOW_MS = 60_000L;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.equals("*")
                ? new String[]{"*"}
                : allowedOrigins.split(",");
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(!allowedOrigins.equals("*"));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalEndpointInterceptor())
                .addPathPatterns("/internal/**");
        registry.addInterceptor(bearerTokenInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health")
                .excludePathPatterns("/api/auth/**");
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/**");
    }

    @Bean
    public HandlerInterceptor bearerTokenInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {
                // Allow OPTIONS (CORS preflight) without auth
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (adminSecret.equals(token)) {
                        return true;
                    }
                }
                log.warn("Unauthorized API access from {}: missing or invalid Bearer token", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized: invalid or missing Bearer token\"}");
                return false;
            }
        };
    }

    @Bean
    public HandlerInterceptor internalEndpointInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {
                String remoteAddr = request.getRemoteAddr();
                if ("127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
                    return true;
                }
                log.warn("Blocked access to /internal from {}", remoteAddr);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return false;
            }
        };
    }

    @Bean
    public HandlerInterceptor rateLimitInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {
                String ip = request.getRemoteAddr();
                long now  = System.currentTimeMillis();

                long[] windowData = rateLimitMap.compute(ip, (k, v) -> {
                    if (v == null || now - v[0] > RATE_LIMIT_WINDOW_MS) {
                        return new long[]{now, 1};
                    }
                    v[1]++;
                    return v;
                });

                if (windowData[1] > RATE_LIMIT_MAX) {
                    log.warn("Rate limit exceeded for IP: {}", ip);
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too Many Requests\"}");
                    return false;
                }
                return true;
            }
        };
    }
}
