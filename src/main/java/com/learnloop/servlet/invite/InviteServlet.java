package com.learnloop.servlet.invite;

import com.learnloop.dao.Database;
import com.learnloop.security.ClientContext;
import com.learnloop.security.CsrfTokens;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.InvitationAccessException;
import com.learnloop.service.InvitationJoinException;
import com.learnloop.service.InvitationService;
import com.learnloop.service.InvitationViews.PublicInvitation;
import com.learnloop.service.ValidationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "InviteServlet", urlPatterns = "/invite/*")
public class InviteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(InviteServlet.class.getName());
    private static final String INVITE_VIEW = "/WEB-INF/views/invite.jsp";

    private volatile InvitationService invitationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareResponse(response);
        String token = tokenFrom(request);
        if (token == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            SessionUser viewer = SessionSecurity.currentUser(request);
            PublicInvitation invitation = invitationService().findPublicInvitation(
                    token, viewer == null ? null : viewer.id());
            SessionSecurity.rememberPostLoginPath(request, "/invite/" + token);
            renderInvitation(request, response, invitation, HttpServletResponse.SC_OK);
        } catch (InvitationAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderUnavailable(request, response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepareResponse(response);
        String token = tokenFrom(request);
        if (token == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        SessionUser user = SessionSecurity.currentUser(request);
        if (user == null || !user.isApproved()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            invitationService().requestToJoin(
                    token,
                    user.id(),
                    request.getParameter("message"),
                    ClientContext.from(request));
            SessionSecurity.clearPostLoginPath(request);
            response.sendRedirect(response.encodeRedirectURL(
                    request.getContextPath() + "/student/classes?requested=1"));
        } catch (InvitationAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (InvitationJoinException exception) {
            if (exception.isAuthorizationFailure()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("error", exception.getMessage());
            showAfterJoinFailure(request, response, token, user, HttpServletResponse.SC_CONFLICT);
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            showAfterJoinFailure(request, response, token, user, HttpServletResponse.SC_BAD_REQUEST);
        } catch (RuntimeException exception) {
            renderUnavailable(request, response, exception);
        }
    }

    private void showAfterJoinFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            String token,
            SessionUser user,
            int status) throws ServletException, IOException {
        try {
            PublicInvitation invitation = invitationService().findPublicInvitation(token, user.id());
            renderInvitation(request, response, invitation, status);
        } catch (InvitationAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderUnavailable(request, response, exception);
        }
    }

    private void renderInvitation(
            HttpServletRequest request,
            HttpServletResponse response,
            PublicInvitation invitation,
            int status) throws ServletException, IOException {
        ResponseSecurity.noStore(response);
        request.setAttribute("pageTitle", "LearnLoop - Class invitation");
        request.setAttribute("csrfToken", CsrfTokens.ensureToken(request.getSession(true)));
        request.setAttribute("invitation", invitation);
        request.setAttribute(
                "invitationAction",
                request.getContextPath() + "/invite/" + tokenFrom(request));
        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        request.getRequestDispatcher(INVITE_VIEW).forward(request, response);
    }

    private void renderUnavailable(
            HttpServletRequest request,
            HttpServletResponse response,
            RuntimeException exception) throws ServletException, IOException {
        LOGGER.log(Level.SEVERE, "Invitation page could not be loaded.", exception);
        ResponseSecurity.noStore(response);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        request.setAttribute("pageTitle", "LearnLoop - Invitation unavailable");
        request.setAttribute(
                "error",
                "This invitation could not be loaded right now. Please try again in a moment.");
        request.getRequestDispatcher(INVITE_VIEW).forward(request, response);
    }

    private static void prepareResponse(HttpServletResponse response) {
        ResponseSecurity.noStore(response);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
    }

    private static String tokenFrom(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2 || pathInfo.charAt(0) != '/') {
            return null;
        }
        String token = pathInfo.substring(1);
        if (token.isEmpty() || token.indexOf('/') >= 0) {
            return null;
        }
        return token;
    }

    private InvitationService invitationService() {
        InvitationService service = invitationService;
        if (service == null) {
            synchronized (this) {
                service = invitationService;
                if (service == null) {
                    service = new InvitationService(Database.fromConfiguration());
                    invitationService = service;
                }
            }
        }
        return service;
    }
}
