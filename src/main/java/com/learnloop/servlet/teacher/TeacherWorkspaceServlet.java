package com.learnloop.servlet.teacher;

import com.learnloop.config.PublicUrlConfigurationException;
import com.learnloop.dao.Database;
import com.learnloop.security.ClientContext;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.AssessmentAccessException;
import com.learnloop.service.AssessmentService;
import com.learnloop.service.ClassAccessException;
import com.learnloop.service.ClassNotFoundException;
import com.learnloop.service.ClassService;
import com.learnloop.service.ClassroomAccessException;
import com.learnloop.service.ClassroomService;
import com.learnloop.service.InvitationService;
import com.learnloop.service.InvitationViews.IssuedInvitation;
import com.learnloop.service.JoinRequestAccessException;
import com.learnloop.service.JoinRequestService;
import com.learnloop.service.LearningAnalyticsService;
import com.learnloop.service.TeacherClassCreateRequest;
import com.learnloop.service.TeacherClassUpdateRequest;
import com.learnloop.service.ValidationException;
import com.learnloop.servlet.workspace.WorkspaceServlet;
import com.learnloop.util.InvitationSharing;
import com.learnloop.util.InvitationTokens;
import com.learnloop.util.MaterialFileStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@WebServlet(name = "TeacherWorkspaceServlet", urlPatterns = "/teacher/*")
@MultipartConfig(
        fileSizeThreshold = 0,
        maxFileSize = MaterialFileStorage.MAX_FILE_SIZE_BYTES,
        maxRequestSize = MaterialFileStorage.MAX_FILE_SIZE_BYTES + 2L * 1024L * 1024L)
public class TeacherWorkspaceServlet extends WorkspaceServlet {

    private static final long serialVersionUID = 1L;
    private static final String CLASSES_PATH = "/teacher/classes";
    private static final String CLASS_DETAIL_PREFIX = "/teacher/classes/";
    private static final String ASSESSMENT_DETAIL_PREFIX = "/teacher/assessments/";
    private static final String REQUESTS_PATH = "/teacher/requests";
    private static final String FLASH_INFO = "teacherClassFlashInfo";
    private static final String INVITATION_SHARE = "teacherInvitationShare";

    private static final Page OVERVIEW = new Page(
            "/WEB-INF/views/teacher/dashboard.jsp",
            "overview",
            "Teacher workspace",
            "Teacher dashboard",
            "Your approval state, assigned subjects, classes and students waiting for a decision.");

    private static final Page CLASSES = new Page(
            "/WEB-INF/views/teacher/classes.jsp",
            "classes",
            "Teacher workspace",
            "Your classes",
            "Create and manage the classes you own, with live enrollment and request totals.");

    private static final Page CLASS_DETAILS = new Page(
            "/WEB-INF/views/teacher/class-details.jsp",
            "classes",
            "Teacher workspace",
            "Class details",
            "Review class information, manage its invitation link or archive it when it is no longer in use.");

    private static final Page REQUESTS = new Page(
            "/WEB-INF/views/teacher/requests.jsp",
            "requests",
            "Teacher workspace",
            "Join requests",
            "Pending requests from students who want to join one of your classes.");

    private static final Page MATERIALS = new Page(
            "/WEB-INF/views/teacher/materials.jsp",
            "materials",
            "Teacher workspace",
            "Class materials",
            "Published and unpublished materials across the classes you own.");

    private static final Page ASSESSMENTS = new Page(
            "/WEB-INF/views/teacher/assessments.jsp",
            "assessments",
            "Teacher workspace",
            "Assessments",
            "Assessments in your classes, their availability windows and attempt activity.");

    private static final Page ASSESSMENT_DETAIL = new Page(
            "/WEB-INF/views/teacher/assessment-detail.jsp",
            "assessments",
            "Teacher workspace",
            "Assessment builder",
            "Prepare questions, publish the assessment, close attempts and grade written responses.");

    private static final Page PROGRESS = new Page(
            "/WEB-INF/views/teacher/progress.jsp",
            "progress",
            "Teacher workspace",
            "Progress reports",
            "Class-level enrollment and graded-assessment summaries for your classes.");

    private static final Map<String, Page> PAGES = Map.of(
            "/teacher/dashboard", OVERVIEW,
            "/teacher/requests", REQUESTS,
            "/teacher/materials", MATERIALS,
            "/teacher/assessments", ASSESSMENTS,
            "/teacher/progress", PROGRESS);

    private volatile ClassService classService;
    private volatile InvitationService invitationService;
    private volatile JoinRequestService joinRequestService;
    private volatile ClassroomService classroomService;
    private volatile AssessmentService assessmentService;
    private volatile LearningAnalyticsService learningAnalyticsService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = SessionSecurity.currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String path = SessionSecurity.requestPath(request);
        if (CLASSES_PATH.equals(path)) {
            showClassList(request, response, user, HttpServletResponse.SC_OK);
            return;
        }
        if (path.startsWith(CLASS_DETAIL_PREFIX)) {
            Long classId = parseDetailId(path);
            if (classId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showClassDetails(request, response, user, classId, HttpServletResponse.SC_OK);
            return;
        }
        if (path.startsWith(ASSESSMENT_DETAIL_PREFIX)) {
            Long assessmentId = parsePathId(path, ASSESSMENT_DETAIL_PREFIX);
            if (assessmentId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showAssessmentDetail(
                    request, response, user, assessmentId, HttpServletResponse.SC_OK);
            return;
        }

        Page page = PAGES.get(path);
        if (page == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        applyPage(request, page);
        consumeFlash(request);
        try {
            switch (page.nav()) {
                case "overview" -> request.setAttribute("overview", dashboardService().teacherOverview(user.id()));
                case "requests" ->
                        request.setAttribute("requests", joinRequestService().findTeacherPendingRequests(user.id()));
                case "materials" -> request.setAttribute("materials", dashboardService().teacherMaterials(user.id()));
                case "assessments" ->
                        request.setAttribute("assessments", dashboardService().teacherAssessments(user.id()));
                case "progress" ->
                        request.setAttribute("analytics", learningAnalyticsService().teacherAnalytics(user.id()));
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
        if (REQUESTS_PATH.equals(path)) {
            handleRequestAction(request, response, user);
            return;
        }
        if (CLASSES_PATH.equals(path)) {
            createClass(request, response, user);
            return;
        }
        if (path.startsWith(CLASS_DETAIL_PREFIX)) {
            Long classId = parseDetailId(path);
            if (classId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            handleClassDetailAction(request, response, user, classId);
            return;
        }
        if (path.startsWith(ASSESSMENT_DETAIL_PREFIX)) {
            Long assessmentId = parsePathId(path, ASSESSMENT_DETAIL_PREFIX);
            if (assessmentId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            handleAssessmentAction(request, response, user, assessmentId);
            return;
        }
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void createClass(HttpServletRequest request, HttpServletResponse response, SessionUser user)
            throws ServletException, IOException {
        try {
            long subjectId = positiveLong(request.getParameter("subjectId"), "subjectId", "Select a subject.");
            Integer maxStudents = optionalCapacity(request.getParameter("maxStudents"));
            ClassService service = classService();
            com.learnloop.model.LearningClass created = service.createManagedClass(
                    new TeacherClassCreateRequest(
                            user.id(),
                            subjectId,
                            request.getParameter("title"),
                            request.getParameter("description"),
                            maxStudents),
                    ClientContext.from(request));
            setFlash(
                    request,
                    "Class created as a draft. Review the details and activate it when you are ready.");
            redirect(response, request.getContextPath() + CLASS_DETAIL_PREFIX + created.id());
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            preserveClassFormValues(request);
            showClassList(request, response, user, HttpServletResponse.SC_BAD_REQUEST);
        } catch (ClassAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASSES, exception);
        }
    }

    private void createAssessment(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        try {
            long assessmentId = assessmentService().createAssessment(
                    user.id(), classId, assessmentForm(request), ClientContext.from(request));
            setFlash(request, "Assessment created as a draft. Add questions, then publish it.");
            redirect(response, request.getContextPath() + ASSESSMENT_DETAIL_PREFIX + assessmentId);
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            preserveAssessmentFormValues(request);
            request.setAttribute("tab", "assessments");
            showClassDetails(request, response, user, classId, HttpServletResponse.SC_BAD_REQUEST);
        } catch (AssessmentAccessException | ClassNotFoundException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ClassAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void handleAssessmentAction(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long assessmentId) throws ServletException, IOException {
        String action = normalizedAction(request.getParameter("action"));
        try {
            AssessmentService service = assessmentService();
            ClientContext clientContext = ClientContext.from(request);
            switch (action) {
                case "update" -> {
                    service.updateAssessment(
                            user.id(), assessmentId, assessmentForm(request), clientContext);
                    setFlash(request, "Assessment details updated.");
                    redirect(response, assessmentUrl(request, assessmentId));
                }
                case "publish" -> {
                    service.publishAssessment(user.id(), assessmentId, clientContext);
                    setFlash(request, "Assessment published. Students can now start attempts.");
                    redirect(response, assessmentUrl(request, assessmentId));
                }
                case "close" -> {
                    service.closeAssessment(user.id(), assessmentId, clientContext);
                    setFlash(request, "Assessment closed. Open attempts were expired.");
                    redirect(response, assessmentUrl(request, assessmentId));
                }
                case "delete" -> {
                    service.deleteAssessment(user.id(), assessmentId, clientContext);
                    setFlash(request, "Draft assessment deleted.");
                    redirect(response, request.getContextPath() + "/teacher/classes");
                }
                case "question-create" -> {
                    service.addQuestion(user.id(), assessmentId, questionForm(request), clientContext);
                    setFlash(request, "Question added.");
                    redirect(response, assessmentUrl(request, assessmentId));
                }
                case "question-delete" -> {
                    long questionId = positiveLong(
                            request.getParameter("questionId"), "questionId", "Select a question.");
                    service.deleteQuestion(user.id(), assessmentId, questionId, clientContext);
                    setFlash(request, "Question deleted.");
                    redirect(response, assessmentUrl(request, assessmentId));
                }
                case "attempt-grade" -> {
                    long attemptId = positiveLong(
                            request.getParameter("attemptId"), "attemptId", "Select an attempt.");
                    service.gradeAttempt(
                            user.id(),
                            assessmentId,
                            attemptId,
                            manualGrades(request),
                            clientContext);
                    setFlash(request, "Attempt graded.");
                    redirect(response, assessmentUrl(request, assessmentId));
                }
                default -> throw new ValidationException("action", "Choose a valid assessment action.");
            }
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            preserveAssessmentFormValues(request);
            preserveQuestionFormValues(request);
            showAssessmentDetail(
                    request, response, user, assessmentId, HttpServletResponse.SC_BAD_REQUEST);
        } catch (AssessmentAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ClassAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (RuntimeException exception) {
            renderFailure(request, response, ASSESSMENT_DETAIL, exception);
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
                    "assessmentDetail", assessmentService().teacherAssessment(user.id(), assessmentId));
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

    private static AssessmentService.AssessmentForm assessmentForm(HttpServletRequest request) {
        return new AssessmentService.AssessmentForm(
                request.getParameter("title"),
                request.getParameter("instructions"),
                request.getParameter("maxAttempts"),
                request.getParameter("timeLimitMinutes"),
                request.getParameter("passingScorePercent"),
                request.getParameter("availableFrom"),
                request.getParameter("dueAt"),
                request.getParameter("revealCorrectAnswers") != null);
    }

    private static AssessmentService.QuestionForm questionForm(HttpServletRequest request) {
        return new AssessmentService.QuestionForm(
                request.getParameter("questionType"),
                request.getParameter("prompt"),
                request.getParameter("points"),
                request.getParameter("required") != null,
                listOf(request.getParameterValues("optionText")),
                listOf(request.getParameterValues("correctOptions")),
                request.getParameter("trueFalseAnswer"));
    }

    private static Map<Long, BigDecimal> manualGrades(HttpServletRequest request) {
        Map<Long, BigDecimal> grades = new LinkedHashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            if (!name.startsWith("points_")) {
                continue;
            }
            String raw = request.getParameter(name);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                grades.put(Long.parseLong(name.substring("points_".length())), new BigDecimal(raw.trim()));
            } catch (NumberFormatException exception) {
                throw new ValidationException("points", "Award points using whole or decimal numbers.");
            }
        }
        return grades;
    }

    private static List<String> listOf(String[] values) {
        return values == null ? List.of() : List.of(values);
    }

    private static void preserveAssessmentFormValues(HttpServletRequest request) {
        request.setAttribute("assessmentFormTitle", request.getParameter("title"));
        request.setAttribute("assessmentFormInstructions", request.getParameter("instructions"));
        request.setAttribute("assessmentFormMaxAttempts", request.getParameter("maxAttempts"));
        request.setAttribute("assessmentFormTimeLimitMinutes", request.getParameter("timeLimitMinutes"));
        request.setAttribute("assessmentFormPassingScorePercent", request.getParameter("passingScorePercent"));
        request.setAttribute("assessmentFormAvailableFrom", request.getParameter("availableFrom"));
        request.setAttribute("assessmentFormDueAt", request.getParameter("dueAt"));
        request.setAttribute(
                "assessmentFormRevealCorrectAnswers",
                request.getParameter("revealCorrectAnswers") != null);
    }

    private static void preserveQuestionFormValues(HttpServletRequest request) {
        request.setAttribute("questionFormType", request.getParameter("questionType"));
        request.setAttribute("questionFormPrompt", request.getParameter("prompt"));
        request.setAttribute("questionFormPoints", request.getParameter("points"));
        request.setAttribute("questionFormRequired", request.getParameter("required") != null);
        request.setAttribute("questionFormOptions", listOf(request.getParameterValues("optionText")));
        request.setAttribute(
                "questionFormCorrectOptions", listOf(request.getParameterValues("correctOptions")));
        request.setAttribute(
                "questionFormTrueFalseAnswer", request.getParameter("trueFalseAnswer"));
    }

    private static String assessmentUrl(HttpServletRequest request, long assessmentId) {
        return request.getContextPath() + ASSESSMENT_DETAIL_PREFIX + assessmentId;
    }

    private void handleClassDetailAction(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        String action = normalizedAction(request.getParameter("action"));
        switch (action) {
            case "meeting-url" -> {
                updateMeetingUrl(request, response, user, classId);
                return;
            }
            case "meeting-room-create" -> {
                createInPlatformMeetingRoom(request, response, user, classId);
                return;
            }
            case "material-create" -> {
                createMaterial(request, response, user, classId);
                return;
            }
            case "material-update" -> {
                updateMaterial(request, response, user, classId);
                return;
            }
            case "material-publish", "material-unpublish", "material-delete" -> {
                updateMaterialState(request, response, user, classId, action);
                return;
            }
            case "assessment-create" -> {
                createAssessment(request, response, user, classId);
                return;
            }
            default -> {
                // Continue with class, invitation and archive actions below.
            }
        }
        updateOwnedClass(request, response, user, classId, action);
    }

    private void updateOwnedClass(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId,
            String action) throws ServletException, IOException {
        try {
            ClassService service = classService();
            ClientContext clientContext = ClientContext.from(request);
            switch (action) {
                case "update" -> {
                    long subjectId = positiveLong(
                            request.getParameter("subjectId"), "subjectId", "Select a subject.");
                    Integer maxStudents = optionalCapacity(request.getParameter("maxStudents"));
                    service.updateClass(
                            new TeacherClassUpdateRequest(
                                    user.id(),
                                    classId,
                                    subjectId,
                                    request.getParameter("title"),
                                    request.getParameter("description"),
                                    maxStudents,
                                    request.getParameter("status")),
                            clientContext);
                    setFlash(request, "Class details updated.");
                    redirect(response, request.getContextPath() + CLASS_DETAIL_PREFIX + classId);
                }
                case "generate-invitation", "regenerate-invitation" -> {
                    boolean regenerate = "regenerate-invitation".equals(action);
                    IssuedInvitation issued = invitationService().issueInvitation(
                            user.id(), classId, regenerate, clientContext);
                    rememberInvitationShare(request, new InvitationShare(
                            classId,
                            issued.url(),
                            InvitationSharing.whatsappUrl(
                                    "Join \"" + issued.classTitle() + "\" on LearnLoop: " + issued.url())));
                    setFlash(
                            request,
                            "Invitation link created. Copy or share it now; it will not be shown again.");
                    redirect(response, request.getContextPath() + CLASS_DETAIL_PREFIX + classId);
                }
                case "revoke-invitation" -> {
                    invitationService().revokeInvitation(user.id(), classId, clientContext);
                    setFlash(request, "Invitation link revoked. Existing links can no longer be used.");
                    redirect(response, request.getContextPath() + CLASS_DETAIL_PREFIX + classId);
                }
                case "expire-invitation" -> {
                    invitationService().expireInvitation(user.id(), classId, clientContext);
                    setFlash(request, "Invitation link expired. Generate a new link when you are ready.");
                    redirect(response, request.getContextPath() + CLASS_DETAIL_PREFIX + classId);
                }
                case "archive" -> {
                    service.archiveClass(user.id(), classId, clientContext);
                    setFlash(request, "Class archived. It remains available for your records.");
                    redirect(response, request.getContextPath() + CLASS_DETAIL_PREFIX + classId);
                }
                case "delete" -> {
                    service.deleteClass(user.id(), classId, clientContext);
                    setFlash(request, "Draft class deleted.");
                    redirect(response, request.getContextPath() + CLASSES_PATH);
                }
                default -> throw new ValidationException("action", "Choose a valid class action.");
            }
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            preserveClassFormValues(request);
            showClassDetails(request, response, user, classId, HttpServletResponse.SC_BAD_REQUEST);
        } catch (ClassNotFoundException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ClassAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (PublicUrlConfigurationException exception) {
            request.setAttribute(
                    "error",
                    "Invitation links are not configured on this server. Set LEARNLOOP_APP_BASE_URL "
                            + "to the public LearnLoop application URL.");
            showClassDetails(request, response, user, classId, HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void createMaterial(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        try {
            classroomService().createMaterial(
                    user.id(),
                    classId,
                    materialForm(request),
                    filePart(request),
                    ClientContext.from(request));
            setFlash(request, "Material added as a draft. Publish it when you are ready.");
            redirect(response, classWorkspaceUrl(request, classId, "materials"));
        } catch (ValidationException exception) {
            showMaterialValidationError(request, response, user, classId, exception, "materials");
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void updateMaterial(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        try {
            long materialId = positiveLong(
                    request.getParameter("materialId"), "materialId", "Select a material.");
            classroomService().updateMaterial(
                    user.id(),
                    classId,
                    materialId,
                    materialForm(request),
                    filePart(request),
                    ClientContext.from(request));
            setFlash(request, "Material updated.");
            redirect(response, classWorkspaceUrl(request, classId, "materials"));
        } catch (ValidationException exception) {
            showMaterialValidationError(request, response, user, classId, exception, "materials");
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void updateMaterialState(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId,
            String action) throws ServletException, IOException {
        try {
            long materialId = positiveLong(
                    request.getParameter("materialId"), "materialId", "Select a material.");
            ClassroomService service = classroomService();
            ClientContext clientContext = ClientContext.from(request);
            switch (action) {
                case "material-publish" -> {
                    service.publishMaterial(user.id(), classId, materialId, clientContext);
                    setFlash(request, "Material published. Enrolled students can now see it.");
                }
                case "material-unpublish" -> {
                    service.unpublishMaterial(user.id(), classId, materialId, clientContext);
                    setFlash(request, "Material unpublished and hidden from students.");
                }
                case "material-delete" -> {
                    service.deleteMaterial(user.id(), classId, materialId, clientContext);
                    setFlash(request, "Material deleted.");
                }
                default -> throw new ValidationException("action", "Choose a valid material action.");
            }
            redirect(response, classWorkspaceUrl(request, classId, "materials"));
        } catch (ValidationException exception) {
            showMaterialValidationError(request, response, user, classId, exception, "materials");
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void createInPlatformMeetingRoom(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        String roomName = "learnloop-class-" + classId + "-" + InvitationTokens.generate();
        String meetingUrl = "https://meet.jit.si/" + roomName;
        try {
            classroomService().updateMeetingUrl(
                    user.id(), classId, meetingUrl, ClientContext.from(request));
            setFlash(request,
                    "In-platform live room created. The class teacher and enrolled students can join from LearnLoop.");
            redirect(response, classWorkspaceUrl(request, classId, "live"));
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            request.setAttribute("tab", "live");
            showClassDetails(request, response, user, classId, HttpServletResponse.SC_BAD_REQUEST);
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void updateMeetingUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId) throws ServletException, IOException {
        String meetingUrl = request.getParameter("meetingUrl");
        try {
            classroomService().updateMeetingUrl(
                    user.id(), classId, meetingUrl, ClientContext.from(request));
            setFlash(request, meetingUrl == null || meetingUrl.isBlank()
                    ? "Live session link cleared."
                    : "Live session link saved.");
            redirect(response, classWorkspaceUrl(request, classId, "live"));
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            request.setAttribute("meetingUrlValue", meetingUrl);
            request.setAttribute("tab", "live");
            showClassDetails(request, response, user, classId, HttpServletResponse.SC_BAD_REQUEST);
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
        }
    }

    private void showMaterialValidationError(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId,
            ValidationException exception,
            String tab) throws ServletException, IOException {
        request.setAttribute("error", exception.getMessage());
        request.setAttribute("fieldErrors", exception.fieldErrors());
        preserveMaterialFormValues(request);
        request.setAttribute("tab", tab);
        showClassDetails(request, response, user, classId, HttpServletResponse.SC_BAD_REQUEST);
    }

    private void handleRequestAction(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user) throws ServletException, IOException {
        try {
            long requestId = positiveLong(
                    request.getParameter("requestId"), "requestId", "Select a valid join request.");
            String action = normalizedAction(request.getParameter("action"));
            JoinRequestService service = joinRequestService();
            ClientContext clientContext = ClientContext.from(request);
            switch (action) {
                case "accept" -> {
                    service.acceptRequest(user.id(), requestId, clientContext);
                    setFlash(request, "Join request accepted. The student has been enrolled.");
                }
                case "reject" -> {
                    service.rejectRequest(user.id(), requestId, request.getParameter("reason"), clientContext);
                    setFlash(request, "Join request rejected. The student has been notified.");
                }
                default -> throw new ValidationException("action", "Choose a valid request action.");
            }
            redirect(response, request.getContextPath() + REQUESTS_PATH);
        } catch (ValidationException exception) {
            request.setAttribute("error", exception.getMessage());
            request.setAttribute("fieldErrors", exception.fieldErrors());
            showRequests(request, response, user, HttpServletResponse.SC_BAD_REQUEST);
        } catch (JoinRequestAccessException exception) {
            request.setAttribute("error", exception.getMessage());
            showRequests(request, response, user, HttpServletResponse.SC_BAD_REQUEST);
        } catch (RuntimeException exception) {
            renderFailure(request, response, REQUESTS, exception);
        }
    }

    private void showRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            int status) throws ServletException, IOException {
        applyPage(request, REQUESTS);
        consumeFlash(request);
        try {
            request.setAttribute("requests", joinRequestService().findTeacherPendingRequests(user.id()));
        } catch (JoinRequestAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, REQUESTS, exception);
            return;
        }
        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        render(request, response, REQUESTS);
    }

    private void showClassList(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            int status) throws ServletException, IOException {
        applyPage(request, CLASSES);
        consumeFlash(request);
        try {
            ClassService service = classService();
            request.setAttribute("classes", service.findOwnedClasses(user.id()));
            request.setAttribute("assignedSubjects", service.findAssignedSubjects(user.id()));
        } catch (ClassAccessException exception) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASSES, exception);
            return;
        }
        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        render(request, response, CLASSES);
    }

    private void showClassDetails(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            long classId,
            int status) throws ServletException, IOException {
        applyPage(request, CLASS_DETAILS);
        consumeFlash(request);
        try {
            ClassService service = classService();
            request.setAttribute("classDetails", service.findOwnedClass(user.id(), classId));
            request.setAttribute("classroom", classroomService().teacherClassroom(user.id(), classId));
            request.setAttribute("assignedSubjects", service.findAssignedSubjects(user.id()));
            request.setAttribute("invitation", invitationService().findTeacherInvitation(user.id(), classId));
            Object tabOverride = request.getAttribute("tab");
            request.setAttribute(
                    "tab",
                    normalizeTab(tabOverride instanceof String value ? value : request.getParameter("tab")));
            InvitationShare share = consumeInvitationShare(request);
            if (share != null && share.classId() == classId) {
                request.setAttribute("invitationLink", share.url());
                request.setAttribute("invitationWhatsAppUrl", share.whatsappUrl());
            }
        } catch (ClassNotFoundException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ClassAccessException | ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, CLASS_DETAILS, exception);
            return;
        }
        if (status != HttpServletResponse.SC_OK) {
            response.setStatus(status);
        }
        render(request, response, CLASS_DETAILS);
    }

    private static void preserveClassFormValues(HttpServletRequest request) {
        request.setAttribute("formTitle", request.getParameter("title"));
        request.setAttribute("formDescription", request.getParameter("description"));
        request.setAttribute("formSubjectId", request.getParameter("subjectId"));
        request.setAttribute("formMaxStudents", request.getParameter("maxStudents"));
        request.setAttribute("formStatus", request.getParameter("status"));
    }

    private static void preserveMaterialFormValues(HttpServletRequest request) {
        request.setAttribute("materialFormTitle", request.getParameter("title"));
        request.setAttribute("materialFormDescription", request.getParameter("description"));
        request.setAttribute("materialFormType", request.getParameter("materialType"));
        request.setAttribute("materialFormResourceUrl", request.getParameter("resourceUrl"));
        request.setAttribute("materialFormContentText", request.getParameter("contentText"));
        request.setAttribute("editingMaterialId", optionalLong(request.getParameter("materialId")));
    }

    private static Long optionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static ClassroomService.MaterialForm materialForm(HttpServletRequest request) {
        return new ClassroomService.MaterialForm(
                request.getParameter("title"),
                request.getParameter("description"),
                request.getParameter("materialType"),
                request.getParameter("resourceUrl"),
                request.getParameter("contentText"));
    }

    private static Part filePart(HttpServletRequest request) {
        try {
            return request.getPart("file");
        } catch (IllegalStateException exception) {
            throw new ValidationException("file", "Files must be 10 MB or smaller.");
        } catch (ServletException | IOException exception) {
            throw new ValidationException("file", "The uploaded file could not be read. Please try again.");
        }
    }

    private static String classWorkspaceUrl(HttpServletRequest request, long classId, String tab) {
        return request.getContextPath() + CLASS_DETAIL_PREFIX + classId + "?tab=" + tab;
    }

    private static String normalizeTab(String tab) {
        if ("materials".equals(tab) || "assessments".equals(tab) || "live".equals(tab)) {
            return tab;
        }
        return "overview";
    }

    private ClassService classService() {
        ClassService service = classService;
        if (service == null) {
            synchronized (this) {
                service = classService;
                if (service == null) {
                    service = new ClassService(Database.fromConfiguration());
                    classService = service;
                }
            }
        }
        return service;
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

    private static void rememberInvitationShare(HttpServletRequest request, InvitationShare share) {
        request.getSession(true).setAttribute(INVITATION_SHARE, share);
    }

    private static InvitationShare consumeInvitationShare(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(INVITATION_SHARE);
        session.removeAttribute(INVITATION_SHARE);
        return value instanceof InvitationShare share ? share : null;
    }

    private static Long parseDetailId(String path) {
        return parsePathId(path, CLASS_DETAIL_PREFIX);
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

    private static Integer optionalCapacity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException(
                    "maxStudents", "Capacity must be a whole number greater than zero.");
        }
    }

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

    private static String normalizedAction(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
    }

    private static void redirect(HttpServletResponse response, String location) throws IOException {
        response.sendRedirect(response.encodeRedirectURL(location));
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

    private static void setFlash(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_INFO, message);
    }

    private record InvitationShare(long classId, String url, String whatsappUrl)
            implements Serializable {

        private static final long serialVersionUID = 1L;
    }
}
