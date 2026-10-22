package co.tz.tfc.ifmis.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory per-IP sliding window limiter for every public portal endpoint. Auth endpoints
 * (which send SMS) get a tight limit; the rest of the API a looser one. Single-instance only — move
 * this to Redis if/when the portal runs on more than one node, so the window is shared.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private static final int AUTH_MAX_REQUESTS = 10;
    private static final int API_MAX_REQUESTS = 120;

    private final ConcurrentHashMap<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * Only trust X-Forwarded-For when the app sits behind our own reverse proxy; otherwise any client
     * could send a fresh value on every request and never be limited.
     */
    @Value("${tfc.rate-limit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        boolean auth = uri.startsWith("/api/auth/");
        String key = (auth ? "auth:" : "api:") + clientIp(request);
        int limit = auth ? AUTH_MAX_REQUESTS : API_MAX_REQUESTS;

        Instant now = Instant.now();
        Deque<Instant> window = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Too many requests, please try again shortly.\"}");
                return;
            }
            window.addLast(now);
        }
        chain.doFilter(request, response);
    }

    /** Drops idle IPs so the map doesn't grow forever. */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void evictIdle() {
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        hits.entrySet().removeIf(entry -> {
            Instant last = entry.getValue().peekLast();
            return last == null || last.isBefore(cutoff);
        });
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (trustForwardedFor && forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
