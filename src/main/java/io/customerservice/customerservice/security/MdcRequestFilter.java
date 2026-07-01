package io.customerservice.customerservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
// Highest precedence so requestId is in MDC before any other filter (including Spring Security) logs anything
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Accept requestId from upstream (API Gateway) to preserve correlation across services,
        // or generate a new one if this is an entry-point request

        String header = request.getHeader(REQUEST_ID_HEADER);
        String requestId;
        try {
            requestId = UUID.fromString(header).toString();
        } catch (IllegalArgumentException | NullPointerException e) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        // Echo the id back so the client can correlate its own logs with ours
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Threads are pooled — without this, the next request on the same thread
            // would inherit the previous request's MDC values
            MDC.clear();
        }
    }
}
