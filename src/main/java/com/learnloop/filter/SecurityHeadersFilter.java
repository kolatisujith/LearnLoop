package com.learnloop.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(
        filterName = "SecurityHeadersFilter",
        urlPatterns = "/*",
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR})
public class SecurityHeadersFilter implements Filter {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; "
            + "base-uri 'self'; "
            + "object-src 'none'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'; "
            + "script-src 'self' https://meet.jit.si https://*.jitsi.net; "
            + "style-src 'self'; "
            + "style-src-elem 'self'; "
            + "style-src-attr 'unsafe-inline'; "
            + "img-src 'self' data: blob: https://meet.jit.si https://*.jitsi.net; "
            + "font-src 'self' data:; "
            + "connect-src 'self' https://meet.jit.si wss://meet.jit.si https://*.jitsi.net wss://*.jitsi.net; "
            + "frame-src https://meet.jit.si https://*.jitsi.net; "
            + "media-src 'self' blob: https://meet.jit.si https://*.jitsi.net; "
            + "worker-src 'self' blob:";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader(
                    "Permissions-Policy",
                    "camera=(self \"https://meet.jit.si\"), microphone=(self \"https://meet.jit.si\"), geolocation=()");
            httpResponse.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);

            if (request instanceof HttpServletRequest httpRequest && httpRequest.isSecure()) {
                httpResponse.setHeader("Strict-Transport-Security", "max-age=15552000");
            }
        }
        chain.doFilter(request, response);
    }

    static String contentSecurityPolicy() {
        return CONTENT_SECURITY_POLICY;
    }
}
