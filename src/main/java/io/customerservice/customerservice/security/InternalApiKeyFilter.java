package io.customerservice.customerservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private final String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String internalAuthHeader = request.getHeader("X-Internal-Api-Key");
        if (internalAuthHeader == null || !MessageDigest.isEqual(
                internalAuthHeader.getBytes(StandardCharsets.UTF_8),
                internalApiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            String clientIp;
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                clientIp = xForwardedFor.split(",")[0].trim()
                        .replaceAll("[^0-9a-fA-F.:% ]", "");
            } else {
                clientIp = request.getRemoteAddr();
            }
            log.warn("internal.unauthorized {} {} from={}", request.getMethod(), request.getRequestURI(),
                    clientIp);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                {"type":"/problems/unauthenticated","status":401,"title":"Unauthorized","detail":"Authentication required","instance":"%s"}
                """.formatted(request.getRequestURI()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
