package com.learnloop.servlet.admin;

import com.learnloop.dao.Database;
import com.learnloop.security.ClientContext;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.AdminAuthorizationException;
import com.learnloop.service.AdminManagementService;
import com.learnloop.service.ClassroomAccessException;
import com.learnloop.service.ClassroomService;
import com.learnloop.service.DashboardService;
import com.learnloop.service.ValidationException;
import com.learnloop.servlet.workspace.WorkspaceServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "AdminWorkspaceServlet", urlPatterns = "/admin/*")
public class AdminWorkspaceServlet extends WorkspaceServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AdminWorkspaceServlet.class.getName());
    private static final String FLASH_INFO = "adminFlashInfo";
    private static final String CLASS_DETAIL_PREFIX = "/admin/classes/";

    private static final Page OVERVIEW = new Page(
            "/WEB-INF/views/admin/dashboard.jsp",
            "overview",
            "Administrator workspace",
            "Administrator dashboard",
            "Live platform totals, approval queues and direct access to the administrative views.");

    private static final Page TEACHERS = new Page(
            "/WEB-INF/views/admin/teachers.jsp",
            "teachers",
            "Administrator workspace",
            "Teacher approvals",
            "Review pending teacher registrations and the current status of every teacher account.");

    private static final Page SUBJECTS = new Page(
            "/WEB-INF/views/admin/subjects.jsp",
            "subjects",
            "Administrator workspace",
            "Subject management",
            "Create subjects, keep their details current and control whether they accept new classes.");

    private static final Page ASSIGNMENTS = new Page(
            "/WEB-INF/views/admin/assignments.jsp",
            "assignments",
            "Administrator workspace",
            "Teacher assignments",
            "Authorise approved teachers to teach subjects. Removing an assignment preserves existing classes.");

    private static final Page USERS = new Page(
            "/WEB-INF/views/admin/users.jsp",
            "users",
            "Administrator workspace",
            "User directory",
            "All LearnLoop accounts with their role, approval state and registration date.");

    private static final Page CLASSES = new Page(
            "/WEB-INF/views/admin/classes.jsp",
            "classes",
            "Administrator workspace",
            "Class overview",
            "Every class, its teacher, enrollment count and pending join requests.");

    private static final Page CLASS_WORKSPACE = new Page(
            "/WEB-INF/views/admin/class-workspace.jsp",
            "classes",
            "Administrator workspace",
            "Class workspace",
            "Inspect class details, every material, assessments and the configured live session.");

    private static final Map<String, Page> PAGES = Map.of(
            "/admin/dashboard", OVERVIEW,
            "/admin/teachers", TEACHERS,
            "/admin/subjects", SUBJECTS,
            "/admin/assignments", ASSIGNMENTS,
            "/admin/users", USERS,
            "/admin/classes", CLASSES);

    private volatile AdminManagementService managementService;
    private volatile ClassroomService classroomService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = SessionSecurity.requestPath(request);
        if (path.startsWith(CLASS_DETAIL_PREFIX)) {
            SessionUser admin = SessionSecurity.currentUser(request);
            if (admin == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Long classId = parseClassId(path);
            if (classId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showClassWorkspace(request, response, admin, classId);
            return;
        }
        Page page = currentPage(request, response);
        if (page == null) {
            return;
        }
        consumeFlash(request);
        showPage(request, response, page, HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Page page = currentPage(request, response);
        if (page == null) {
            return;
        }
        SessionUser admin = SessionSecurity.currentUser(request);
        if (admin == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            String message = performAction(request, page, admin);
            setFlash(request, FLASH_INFO, message);
            redirectToPage(request, response, page);
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            preserveSubmittedValues(request);
            showPage(request, response, page, HttpServletResponse.SC_BAD_REQUEST);
        } catch (AdminAuthorizationException exception) {
            LOGGER.log(Level.WARNING, "Rejected an unauthorised administrator action.", exception);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (RuntimeException exception) {
            renderFailure(request, response, page, exception);
        }
    }

    private String performAction(HttpServletRequest request, Page page, SessionUser admin) {
        String action = normalizedAction(request.getParameter("action"));
        ClientContext clientContext = ClientContext.from(request);
        AdminManagementService service = managementService();

        return switch (page.nav()) {
            case "teachers" -> {
                long teacherId = positiveLong(request.getParameter("teacherId"), "teacherId", "Select a teacher.");
                if ("approve".equals(action)) {
                    service.approveTeacher(admin.id(), teacherId, clientContext);
                    yield "Teacher account approved.";
                }
                if ("reject".equals(action)) {
                    service.rejectTeacher(admin.id(), teacherId, request.getParameter("reason"), clientContext);
                    yield "Teacher application rejected and the reason was recorded.";
                }
                throw unsupportedAction();
            }
            case "subjects" -> {
                switch (action) {
                    case "create" -> {
                        service.createSubject(
                                admin.id(),
                                request.getParameter("code"),
                                request.getParameter("name"),
                                request.getParameter("description"),
                                clientContext);
                        yield "Subject created.";
                    }
                    case "update" -> {
                        long subjectId = positiveLong(
                                request.getParameter("subjectId"), "subjectId", "Select a subject.");
                        service.updateSubject(
                                admin.id(),
                                subjectId,
                                request.getParameter("code"),
                                request.getParameter("name"),
                                request.getParameter("description"),
                                clientContext);
                        yield "Subject details updated.";
                    }
                    case "activate", "deactivate" -> {
                        long subjectId = positiveLong(
                                request.getParameter("subjectId"), "subjectId", "Select a subject.");
                        boolean active = "activate".equals(action);
                        service.setSubjectActive(admin.id(), subjectId, active, clientContext);
                        yield active ? "Subject activated." : "Subject deactivated.";
                    }
                    default -> throw unsupportedAction();
                }
            }
            case "assignments" -> {
                long teacherId = positiveLong(
                        request.getParameter("teacherId"), "teacherId", "Select a teacher.");
                long subjectId = positiveLong(
                        request.getParameter("subjectId"), "subjectId", "Select a subject.");
                if ("assign".equals(action)) {
                    service.assignTeacher(admin.id(), teacherId, subjectId, clientContext);
                    yield "Teacher assigned to the subject.";
                }
                if ("remove".equals(action)) {
                    service.removeAssignment(admin.id(), teacherId, subjectId, clientContext);
                    yield "Assignment deactivated. Existing classes remain intact.";
                }
                throw unsupportedAction();
            }
            default -> throw new ValidationException("action", "This admin page does not accept changes.");
        };
    }

    private void showPage(
            HttpServletRequest request,
            HttpServletResponse response,
            Page page,
            int status) throws ServletException, IOException {
        applyPage(request, page);
        String query = request.getParameter("q");
        int requestedPage = parsePage(request.getParameter("page"));
        request.setAttribute("query", query == null ? "" : query.trim());
        request.setAttribute("pageNumber", requestedPage);

        try {
            switch (page.nav()) {
                case "overview" -> request.setAttribute("overview", dashboardService().adminOverview());
                case "teachers" -> request.setAttribute(
                        "teachers", dashboardService().adminTeachers(query, requestedPage));
                case "subjects" -> request.setAttribute(
                        "subjects", dashboardService().adminSubjects(query, requestedPage));
                case "assignments" -> {
                    request.setAttribute(
                            "assignments", dashboardService().adminAssignments(query, requestedPage));
                    request.setAttribute("approvedTeachers", dashboardService().adminApprovedTeachers());
                    request.setAttribute("activeSubjects", dashboardService().adminActiveSubjects());
                }
                case "users" -> request.setAttribute("users", dashboardService().adminUsers(query, requestedPage));
                case "classes" -> request.setAttribute(
                        "classes", dashboardService().adminClasses(query, requestedPage));
                default -> {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (RuntimeException exception) {
            renderFailure(request, response, page, exception);
            return;
        }

        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        render(request, response, page);
    }

    private void showClassWorkspace(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser admin,
            long classId) throws ServletException, IOException {
        applyPage(request, CLASS_WORKSPACE);
        consumeFlash(request);
        try {
            request.setAttribute("classroom", classroomService().adminClassroom(admin.id(), classId));
            request.setAttribute("tab", normalizeTab(request.getParameter("tab")));
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_WORKSPACE, exception);
            return;
        }
        render(request, response, CLASS_WORKSPACE);
    }

    private static Long parseClassId(String path) {
        String value = path.substring(CLASS_DETAIL_PREFIX.length());
        if (value.isEmpty() || value.indexOf('/') >= 0) {
            return null;
        }
        try {
            long classId = Long.parseLong(value);
            return classId > 0 ? classId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String normalizeTab(String tab) {
        if ("materials".equals(tab) || "assessments".equals(tab) || "live".equals(tab)) {
            return tab;
        }
        return "overview";
    }

    private Page currentPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Page page = PAGES.get(SessionSecurity.requestPath(request));
        if (page == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        if (SessionSecurity.currentUser(request) == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return page;
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

    private AdminManagementService managementService() {
        AdminManagementService service = managementService;
        if (service == null) {
            synchronized (this) {
                service = managementService;
                if (service == null) {
                    service = new AdminManagementService(Database.fromConfiguration());
                    managementService = service;
                }
            }
        }
        return service;
    }

    private void redirectToPage(HttpServletRequest request, HttpServletResponse response, Page page)
            throws IOException {
        String path = pathFor(page);
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + path));
    }

    private static String pathFor(Page page) {
        return PAGES.entrySet().stream()
                .filter(entry -> entry.getValue() == page)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("/admin/dashboard");
    }

    private static void consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object info = session.getAttribute(FLASH_INFO);
        if (info instanceof String message && !message.isBlank()) {
            request.setAttribute("info", message);
        }
        session.removeAttribute(FLASH_INFO);
    }

    private static void setFlash(HttpServletRequest request, String key, String message) {
        HttpSession session = request.getSession(true);
        session.setAttribute(key, message);
    }

    private static void preserveSubmittedValues(HttpServletRequest request) {
        request.setAttribute("formTeacherId", request.getParameter("teacherId"));
        request.setAttribute("formSubjectId", request.getParameter("subjectId"));
        request.setAttribute("formReason", request.getParameter("reason"));
        request.setAttribute("formCode", request.getParameter("code"));
        request.setAttribute("formName", request.getParameter("name"));
        request.setAttribute("formDescription", request.getParameter("description"));
        request.setAttribute("submittedAction", request.getParameter("action"));
    }

    private static String normalizedAction(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
    }

    private static int parsePage(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static long positiveLong(String value, String field, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, message);
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new ValidationException(field, message);
    }

    private static ValidationException unsupportedAction() {
        return new ValidationException("action", "Choose a valid admin action.");
    }
}
