package com.learnloop.servlet.student;

import com.learnloop.dao.Database;
import com.learnloop.security.ClientContext;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.AssessmentAccessException;
import com.learnloop.service.AssessmentService;
import com.learnloop.service.ClassroomAccessException;
import com.learnloop.service.ClassroomService;
import com.learnloop.service.JoinRequestAccessException;
import com.learnloop.service.JoinRequestService;
import com.learnloop.service.LearningAnalyticsService;
import com.learnloop.service.ValidationException;
import com.learnloop.servlet.workspace.WorkspaceServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@WebServlet(name = "StudentWorkspaceServlet", urlPatterns = "/student/*")
public class StudentWorkspaceServlet extends WorkspaceServlet {

    private static final long serialVersionUID = 1L;
    private static final String CLASS_DETAIL_PREFIX = "/student/classes/";
    private static final String ASSESSMENT_DETAIL_PREFIX = "/student/assessments/";
    private static final String ATTEMPT_DETAIL_PREFIX = "/student/attempts/";
    private static final String FLASH_INFO = "studentAssessmentFlashInfo";

    private static final Page OVERVIEW = new Page(
            "/WEB-INF/views/student/dashboard.jsp",
            "overview",
            "Student workspace",
            "Student dashboard",
            "Your enrolled classes, pending requests, available assessments and recent results.");

    private static final Page CLASSES = new Page(
            "/WEB-INF/views/student/classes.jsp",
            "classes",
            "Student workspace",
            "My classes",
            "The classes you are enrolled in and any requests still awaiting review.");

    private static final Page REQUESTS = new Page(
            "/WEB-INF/views/student/requests.jsp",
            "requests",
            "Student workspace",
            "My join requests",
            "Every class join request you have submitted, with its current status and review date.");

    private static final Page DISCOVER = new Page(
            "/WEB-INF/views/student/discover.jsp",
            "discover",
            "Student workspace",
            "Discover classes",
            "Active LearnLoop classes that are open to discovery outside your current enrollments.");

    private static final Page LEARNING = new Page(
            "/WEB-INF/views/student/learning.jsp",
            "learning",
            "Student workspace",
            "Learning area",
            "Materials and assessments from every class in which you are actively enrolled.");

    private static final Page ASSESSMENTS = new Page(
            "/WEB-INF/views/student/assessments.jsp",
            "assessments",
            "Student workspace",
            "My assessments",
            "Assessments currently available to you and your most recent submitted results.");

    private static final Page ANALYTICS = new Page(
            "/WEB-INF/views/student/analytics.jsp",
            "analytics",
            "Student workspace",
            "Learning analytics",
            "Review your assessment outcomes and get suggested next steps based on your learning activity.");

    private static final Page CLASS_WORKSPACE = new Page(
            "/WEB-INF/views/student/class-workspace.jsp",
            "classes",
            "Student workspace",
            "Class workspace",
            "Published materials, assessments and the live session for this class.");

    private static final Page ASSESSMENT_DETAIL = new Page(
            "/WEB-INF/views/student/assessment-detail.jsp",
            "assessments",
            "Student workspace",
            "Assessment",
            "Instructions, availability, attempt history and results for this assessment.");

    private static final Page ATTEMPT = new Page(
            "/WEB-INF/views/student/assessment-attempt.jsp",
            "assessments",
            "Student workspace",
            "Assessment attempt",
            "Answer each question, submit before time runs out and review your result.");

    private static final Map<String, Page> PAGES = Map.of(
            "/student/dashboard", OVERVIEW,
            "/student/classes", CLASSES,
            "/student/requests", REQUESTS,
            "/student/discover", DISCOVER,
            "/student/learning", LEARNING,
            "/student/assessments", ASSESSMENTS,
            "/student/analytics", ANALYTICS);

    private volatile JoinRequestService joinRequestService;
    private volatile ClassroomService classroomService;
    private volatile AssessmentService assessmentService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = SessionSecurity.currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String path = SessionSecurity.requestPath(request);
        if (path.startsWith(CLASS_DETAIL_PREFIX)) {
            Long classId = parsePathId(path, CLASS_DETAIL_PREFIX);
            if (classId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showClassWorkspace(request, response, user, classId);
            return;
        }
        if (path.startsWith(ASSESSMENT_DETAIL_PREFIX)) {
            Long assessmentId = parsePathId(path, ASSESSMENT_DETAIL_PREFIX);
            if (assessmentId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showAssessmentDetail(request, response, user, assessmentId, HttpServletResponse.SC_OK);
            return;
        }
        if (path.startsWith(ATTEMPT_DETAIL_PREFIX)) {
            Long attemptId = parsePathId(path, ATTEMPT_DETAIL_PREFIX);
            if (attemptId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showAttempt(request, response, user, attemptId, HttpServletResponse.SC_OK);
            return;
        }

        Page page = PAGES.get(path);
        if (page == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        applyPage(request, page);
        if ("1".equals(request.getParameter("requested"))) {
            request.setAttribute(
                    "info",
                    "Your request to join the class was submitted. The teacher will review it.");
        }
        try {
            switch (page.nav()) {
                case "overview" -> request.setAttribute("overview", dashboardService().studentOverview(user.id()));
                case "classes" -> {
                    request.setAttribute("classes", dashboardService().studentClasses(user.id()));
                    request.setAttribute("pendingRequests", joinRequestService().findStudentPendingRequests(user.id()));
                }
                case "requests" -> request.setAttribute("requests", joinRequestService().findStudentRequests(user.id()));
                case "discover" -> request.setAttribute("classes", dashboardService().discoverClasses(user.id()));
                case "learning" -> request.setAttribute("materials", dashboardService().studentMaterials(user.id()));
                case "assessments" ->
                        request.setAttribute("assessments", dashboardService().studentAssessments(user.id()));
                case "analytics" ->
                        request.setAttribute("analytics", learningAnalyticsService().studentAnalytics(
                                user.id(), dashboardService().studentAssessments(user.id())));
                default -> {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (JoinRequestAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, page, exception);
            return;
        }
        render(request, response, page);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = SessionSecurity.currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String path = SessionSecurity.requestPath(request);
        if (path.startsWith(ASSESSMENT_DETAIL_PREFIX)) {
            Long assessmentId = parsePathId(path, ASSESSMENT_DETAIL_PREFIX);
            if (assessmentId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            startAttempt(request, response, user, assessmentId);
            return;
        }
        if (path.startsWith(ATTEMPT_DETAIL_PREFIX)) {
            Long attemptId = parsePathId(path, ATTEMPT_DETAIL_PREFIX);
            if (attemptId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            submitAttempt(request, response, user, attemptId);
            return;
        }
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void startAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long assessmentId) throws ServletException, IOException {
        if (!"start".equals(normalizedAction(request.getParameter("action")))) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            long attemptId = assessmentService().startAttempt(
                    user.id(), assessmentId, ClientContext.from(request));
            redirect(response, request.getContextPath() + ATTEMPT_DETAIL_PREFIX + attemptId);
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            showAssessmentDetail(
                    request, response, user, assessmentId, HttpServletResponse.SC_BAD_REQUEST);
        } catch (AssessmentAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, ASSESSMENT_DETAIL, exception);
        }
    }

    private void submitAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long attemptId) throws ServletException, IOException {
        Map<Long, String> textAnswers = submittedTextAnswers(request);
        Map<Long, Set<Long>> selectedOptions = submittedOptions(request);
        try {
            assessmentService().submitAttempt(
                    user.id(),
                    attemptId,
                    textAnswers,
                    selectedOptions,
                    ClientContext.from(request));
            setFlash(request, "Attempt submitted.");
            redirect(response, request.getContextPath() + ATTEMPT_DETAIL_PREFIX + attemptId);
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            showAttempt(
                    request,
                    response,
                    user,
                    attemptId,
                    textAnswers,
                    selectedOptions,
                    HttpServletResponse.SC_BAD_REQUEST);
        } catch (AssessmentAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, ATTEMPT, exception);
        }
    }

    private void showAssessmentDetail(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long assessmentId,
            int status) throws ServletException, IOException {
        applyPage(request, ASSESSMENT_DETAIL);
        consumeFlash(request);
        try {
            request.setAttribute(
                    "assessmentDetail", assessmentService().studentAssessment(user.id(), assessmentId));
        } catch (AssessmentAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, ASSESSMENT_DETAIL, exception);
            return;
        }
        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        render(request, response, ASSESSMENT_DETAIL);
    }

    private void showAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long attemptId,
            int status) throws ServletException, IOException {
        showAttempt(request, response, user, attemptId, Map.of(), Map.of(), status);
    }

    private void showAttempt(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long attemptId,
            Map<Long, String> textAnswers,
            Map<Long, Set<Long>> selectedOptions,
            int status) throws ServletException, IOException {
        applyPage(request, ATTEMPT);
        consumeFlash(request);
        try {
            request.setAttribute(
                    "attemptPage",
                    textAnswers.isEmpty() && selectedOptions.isEmpty()
                            ? assessmentService().studentAttempt(user.id(), attemptId)
                            : assessmentService().studentAttempt(
                                    user.id(), attemptId, textAnswers, selectedOptions));
        } catch (AssessmentAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, ATTEMPT, exception);
            return;
        }
        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        render(request, response, ATTEMPT);
    }

    private static Map<Long, String> submittedTextAnswers(HttpServletRequest request) {
        Map<Long, String> answers = new LinkedHashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            Long questionId = parameterId(name, "answer_");
            if (questionId != null) {
                answers.put(questionId, request.getParameter(name));
            }
        }
        return answers;
    }

    private static Map<Long, Set<Long>> submittedOptions(HttpServletRequest request) {
        Map<Long, Set<Long>> selections = new LinkedHashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            Long questionId = parameterId(name, "option_");
            if (questionId == null) {
                continue;
            }
            Set<Long> optionIds = selections.computeIfAbsent(questionId, ignored -> new LinkedHashSet<>());
            for (String value : request.getParameterValues(name)) {
                try {
                    long optionId = Long.parseLong(value);
                    if (optionId > 0) {
                        optionIds.add(optionId);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return selections;
    }

    private static Long parameterId(String name, String prefix) {
        if (!name.startsWith(prefix)) {
            return null;
        }
        try {
            long id = Long.parseLong(name.substring(prefix.length()));
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String normalizedAction(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
    }

    private void showClassWorkspace(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        applyPage(request, CLASS_WORKSPACE);
        try {
            request.setAttribute("classroom", classroomService().studentClassroom(user.id(), classId));
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

    private static Long parsePathId(String path, String prefix) {
        String value = path.substring(prefix.length());
        if (value.isEmpty() || value.indexOf('/') >= 0) {
            return null;
        }
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static void redirect(HttpServletResponse response, String location) throws IOException {
        response.sendRedirect(response.encodeRedirectURL(location));
    }

    private static void consumeFlash(HttpServletRequest request) {
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object info = session.getAttribute(FLASH_INFO);
        if (info instanceof String message && !message.isBlank()) {
            request.setAttribute("info", message);
        }
        session.removeAttribute(FLASH_INFO);
    }

    private static void setFlash(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_INFO, message);
    }

    private static String normalizeTab(String tab) {
        if ("materials".equals(tab) || "assessments".equals(tab) || "live".equals(tab)) {
            return tab;
        }
        return "overview";
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

    private AssessmentService assessmentService() {
        AssessmentService service = assessmentService;
        if (service == null) {
            synchronized (this) {
                service = assessmentService;
                if (service == null) {
                    service = new AssessmentService(Database.fromConfiguration());
                    assessmentService = service;
                }
            }
        }
        return service;
    }

    private volatile LearningAnalyticsService learningAnalyticsService;

    private LearningAnalyticsService learningAnalyticsService() {
        LearningAnalyticsService service = learningAnalyticsService;
        if (service == null) {
            synchronized (this) {
                service = learningAnalyticsService;
                if (service == null) {
                    service = new LearningAnalyticsService(Database.fromConfiguration());
                    learningAnalyticsService = service;
                }
            }
        }
        return service;
    }

    private JoinRequestService joinRequestService() {
        JoinRequestService service = joinRequestService;
        if (service == null) {
            synchronized (this) {
                service = joinRequestService;
                if (service == null) {
                    service = new JoinRequestService(Database.fromConfiguration());
                    joinRequestService = service;
                }
            }
        }
        return service;
    }
}
