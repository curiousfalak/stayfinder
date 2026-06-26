package com.example.stayfinder.backend.config;


import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100,
                        Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;
        String ip = httpRequest.getRemoteAddr();

        Bucket bucket = buckets.computeIfAbsent(
                ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse =
                    (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.getWriter()
                    .write("{\"error\":\"Too many requests\"}");
        }
    }
}