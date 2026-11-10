package com.learnloop.filter;

import com.learnloop.security.CsrfTokens;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@WebFilter(
        filterName = "CsrfFilter",
        urlPatterns = "/*",
        dispatcherTypes = DispatcherType.REQUEST)
public class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String method = httpRequest.getMethod().toUpperCase(Locale.ROOT);

        if ("TRACE".equals(method)) {
            httpResponse.setHeader("Allow", "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS");
            httpResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (!isSafeMethod(method)) {
            HttpSession session = httpRequest.getSession(false);
            String submitted = httpRequest.getHeader(CsrfTokens.HEADER_NAME);
            if (submitted == null || submitted.isBlank()) {
                submitted = httpRequest.getParameter(CsrfTokens.PARAMETER_NAME);
            }
            if (!CsrfTokens.isValid(session, submitted)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    static boolean isSafeMethod(String method) {
        return method != null && SAFE_METHODS.contains(method.toUpperCase(Locale.ROOT));
    }
}
