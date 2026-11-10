package com.learnloop.filter;

import com.learnloop.model.UserRole;
import com.learnloop.security.ProtectedRoutes;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(
        filterName = "RoleAuthorizationFilter",
        urlPatterns = {
                "/admin/*",
                "/teacher/*",
                "/student/*",
                "/WEB-INF/views/admin/*",
                "/WEB-INF/views/teacher/*",
                "/WEB-INF/views/student/*"
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class RoleAuthorizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        ResponseSecurity.noStore(httpResponse);

        UserRole requiredRole = ProtectedRoutes.requiredRole(SessionSecurity.requestPath(httpRequest));
        if (requiredRole == null) {
            chain.doFilter(request, response);
            return;
        }

        SessionUser user = SessionSecurity.currentUser(httpRequest);
        if (user == null || !user.isApproved()) {
            if (user != null) {
                SessionSecurity.signOut(httpRequest);
            }
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(AuthRedirects.loginUrl(httpRequest)));
            return;
        }

        if (user.role() != requiredRole) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }
}
