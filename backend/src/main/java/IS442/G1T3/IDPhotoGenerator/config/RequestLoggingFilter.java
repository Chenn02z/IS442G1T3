package IS442.G1T3.IDPhotoGenerator.config;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        
        String queryString = requestWrapper.getQueryString() != null ? "?" + requestWrapper.getQueryString() : "";
        String requestPath = requestWrapper.getRequestURI() + queryString;
        
        log.info("INCOMING REQUEST: {} {} (client: {})",
                requestWrapper.getMethod(),
                requestPath,
                requestWrapper.getRemoteAddr());
        
        Map<String, String[]> params = requestWrapper.getParameterMap();
        if (!params.isEmpty()) {
            String paramsStr = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                    .collect(Collectors.joining(", "));
            log.info("PARAMETERS: {}", paramsStr);
        }
        
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("RESPONSE: {} {} - {} ({} ms)",
                    requestWrapper.getMethod(),
                    requestPath,
                    responseWrapper.getStatus(),
                    duration);
            
            responseWrapper.copyBodyToResponse();
        }
    }
} 