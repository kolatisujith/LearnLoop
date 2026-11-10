package com.learnloop.servlet;

import com.learnloop.dao.Database;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.ClassroomAccessException;
import com.learnloop.service.ClassroomService;
import com.learnloop.util.ExternalResourceUrls;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Opens an authorized class meeting. Google Meet sessions redirect externally; Jitsi demo rooms
 * are embedded in the LearnLoop page using the Jitsi IFrame API.
 */
@WebServlet(name = "MeetingServlet", urlPatterns = "/meetings/*")
public class MeetingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String MEETING_PREFIX = "/meetings/";

    private volatile ClassroomService classroomService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        SessionUser user = SessionSecurity.currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String path = SessionSecurity.requestPath(request);
        if (!path.startsWith(MEETING_PREFIX)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String idPart = path.substring(MEETING_PREFIX.length());
        long classId;
        try {
            classId = Long.parseLong(idPart);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (classId <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            String meetingUrl = classroomService().openMeetingUrl(user.id(), user.role(), classId);
            ResponseSecurity.noStore(response);
            if (ExternalResourceUrls.isJitsiMeetUrl(meetingUrl)) {
                request.setAttribute("meetingRoomName", ExternalResourceUrls.jitsiRoomName(meetingUrl));
                request.setAttribute("meetingDisplayName", user.fullName());
                request.setAttribute("meetingClassId", classId);
                request.setAttribute("meetingReturnUrl", returnUrl(request, user, classId));
                request.getRequestDispatcher("/WEB-INF/views/live-meeting.jsp").forward(request, response);
                return;
            }
            response.sendRedirect(meetingUrl);
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private static String returnUrl(HttpServletRequest request, SessionUser user, long classId) {
        String prefix = switch (user.role()) {
            case ADMIN -> "/admin/classes/";
            case TEACHER -> "/teacher/classes/";
            case STUDENT -> "/student/classes/";
        };
        return request.getContextPath() + prefix + classId + "?tab=live";
    }

    private ClassroomService classroomService() {
        ClassroomService service = classroomService;
        if (service == null) {
            synchronized (this) {
                service = classroomService;
                if (service == null) {
                    service = new ClassroomService(Database.fromConfiguration());
                    classroomService = service;
                }
            }
        }
        return service;
    }
}
