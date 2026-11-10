package com.learnloop.servlet.auth;

import com.learnloop.dao.Database;
import com.learnloop.security.ClientContext;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.AuthenticationService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LogoutServlet", urlPatterns = "/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LogoutServlet.class.getName());

    private volatile AuthenticationService authenticationService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ResponseSecurity.noStore(response);
        SessionUser user = SessionSecurity.currentUser(request);
        if (user != null) {
            try {
                authenticationService().recordLogout(user, ClientContext.from(request));
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Logout audit could not be written.", exception);
            }
        }
        SessionSecurity.signOut(request);
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/login?logout=1"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ResponseSecurity.noStore(response);
        response.setHeader("Allow", "POST");
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private AuthenticationService authenticationService() {
        AuthenticationService service = authenticationService;
        if (service == null) {
            synchronized (this) {
                service = authenticationService;
                if (service == null) {
                    service = new AuthenticationService(Database.fromConfiguration());
                    authenticationService = service;
                }
            }
        }
        return service;
    }
}
