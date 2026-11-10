package com.learnloop.servlet.auth;

import com.learnloop.dao.DataAccessException;
import com.learnloop.dao.Database;
import com.learnloop.model.UserAccount;
import com.learnloop.model.UserRole;
import com.learnloop.security.ClientContext;
import com.learnloop.security.CsrfTokens;
import com.learnloop.security.ProtectedRoutes;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.RegistrationRequest;
import com.learnloop.service.UserService;
import com.learnloop.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "RegistrationServlet", urlPatterns = "/register")
public class RegistrationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RegistrationServlet.class.getName());
    private static final String REGISTER_VIEW = "/WEB-INF/views/auth/register.jsp";

    private volatile UserService userService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResponseSecurity.noStore(response);
        SessionUser currentUser = SessionSecurity.currentUser(request);
        if (currentUser != null) {
            String dashboard = ProtectedRoutes.dashboardFor(currentUser.role());
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + dashboard));
            return;
        }

        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CsrfTokens.ensureToken(session));
        request.setAttribute("formRole", UserRole.STUDENT.name());
        request.getRequestDispatcher(REGISTER_VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ResponseSecurity.noStore(response);

        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");
        String role = request.getParameter("role");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        try {
            UserAccount account = userService().register(
                    new RegistrationRequest(email, fullName, role, password, confirmPassword),
                    ClientContext.from(request));

            String outcome = account.role() == UserRole.TEACHER ? "teacher" : "student";
            response.sendRedirect(response.encodeRedirectURL(
                    request.getContextPath() + "/login?registered=" + outcome));
        } catch (ValidationException exception) {
            showErrors(request, response, email, fullName, role, exception);
        } catch (DataAccessException exception) {
            LOGGER.log(Level.SEVERE, "Registration failed because of a database error.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Registration failed because of a configuration error.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void showErrors(
            HttpServletRequest request,
            HttpServletResponse response,
            String email,
            String fullName,
            String role,
            ValidationException exception) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("csrfToken", CsrfTokens.ensureToken(session));
        request.setAttribute("fieldErrors", exception.fieldErrors());
        request.setAttribute("error", "Please correct the highlighted fields and try again.");
        request.setAttribute("formEmail", email == null ? "" : email.trim());
        request.setAttribute("formFullName", fullName == null ? "" : fullName.trim());
        request.setAttribute("formRole", normalizeRole(role));
        request.getRequestDispatcher(REGISTER_VIEW).forward(request, response);
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return UserRole.STUDENT.name();
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return "TEACHER".equals(normalized) ? UserRole.TEACHER.name() : UserRole.STUDENT.name();
    }

    private UserService userService() {
        UserService service = userService;
        if (service == null) {
            synchronized (this) {
                service = userService;
                if (service == null) {
                    service = new UserService(Database.fromConfiguration());
                    userService = service;
                }
            }
        }
        return service;
    }
}
