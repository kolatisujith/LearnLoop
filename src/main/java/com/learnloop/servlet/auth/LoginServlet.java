package com.learnloop.servlet.auth;

import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.security.ClientContext;
import com.learnloop.security.CsrfTokens;
import com.learnloop.security.ProtectedRoutes;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SafeRedirects;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.AuthenticationService;
import com.learnloop.service.LoginAttempt;
import com.learnloop.service.LoginResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String LOGIN_VIEW = "/WEB-INF/views/auth/login.jsp";

    private volatile AuthenticationService authenticationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResponseSecurity.noStore(response);
        SessionUser currentUser = SessionSecurity.currentUser(request);
        if (currentUser != null) {
            redirectToDashboard(request, response, currentUser);
            return;
        }

        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CsrfTokens.ensureToken(session));
        String nextPath = SafeRedirects.safeLocalPath(request.getParameter("next"));
        boolean inviteReturn = false;
        if (nextPath == null && "1".equals(request.getParameter("invite"))) {
            nextPath = SessionSecurity.postLoginPath(request);
            inviteReturn = nextPath != null;
        }
        request.setAttribute("nextPath", inviteReturn ? null : nextPath);
        request.setAttribute("inviteReturn", inviteReturn);
        applyQueryMessage(request);
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResponseSecurity.noStore(response);

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String nextPath = SafeRedirects.safeLocalPath(request.getParameter("next"));
        boolean inviteReturn = false;
        if (nextPath == null && "1".equals(request.getParameter("invite"))) {
            nextPath = SessionSecurity.postLoginPath(request);
            inviteReturn = nextPath != null;
        }

        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            showError(
                    request, response, email, nextPath, inviteReturn,
                    "Enter your email address and password.");
            return;
        }

        try {
            ClientContext clientContext = ClientContext.from(request);
            LoginResult result = authenticationService().authenticate(new LoginAttempt(
                    email,
                    password,
                    clientContext.ipAddress(),
                    clientContext.userAgent()));

            if (!result.status().isSuccess()) {
                showError(
                        request, response, email, nextPath, inviteReturn, result.status().message());
                return;
            }

            SessionSecurity.signIn(request, result.user());
            SessionSecurity.clearPostLoginPath(request);
            if (nextPath != null) {
                response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + nextPath));
                return;
            }
            redirectToDashboard(request, response, result.user());
        } catch (DataAccessException | IllegalStateException exception) {
            LOGGER.log(Level.SEVERE, "Login could not be completed because of a server-side failure.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Login could not be completed because of a configuration failure.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void showError(
            HttpServletRequest request,
            HttpServletResponse response,
            String email,
            String nextPath,
            boolean inviteReturn,
            String message) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CsrfTokens.ensureToken(session));
        request.setAttribute("error", message);
        request.setAttribute("formEmail", email == null ? "" : email.trim());
        request.setAttribute("nextPath", inviteReturn ? null : nextPath);
        request.setAttribute("inviteReturn", inviteReturn);
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }

    private void redirectToDashboard(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user) throws IOException {
        String dashboard = ProtectedRoutes.dashboardFor(user.role());
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + dashboard));
    }

    private static void applyQueryMessage(HttpServletRequest request) {
        if (request.getParameter("logout") != null) {
            request.setAttribute("info", "You have been signed out.");
        } else if ("teacher".equals(request.getParameter("registered"))) {
            request.setAttribute("info",
                    "Your teacher account was created and is pending administrator approval.");
        } else if ("student".equals(request.getParameter("registered"))) {
            request.setAttribute("info", "Your student account was created. Sign in to continue.");
        }
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
