package com.learnloop.servlet;

import com.learnloop.dao.Database;
import com.learnloop.model.UserRole;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.security.SessionSecurity;
import com.learnloop.security.SessionUser;
import com.learnloop.service.ClassroomAccessException;
import com.learnloop.service.ClassroomService;
import com.learnloop.service.ClassroomViews.Material;
import com.learnloop.service.ClassroomViews.MaterialAccess;
import com.learnloop.servlet.workspace.WorkspaceServlet;
import com.learnloop.util.MaterialFileStorage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet(name = "MaterialServlet", urlPatterns = "/materials/*")
public class MaterialServlet extends WorkspaceServlet {

    private static final long serialVersionUID = 1L;
    private static final String MATERIAL_PREFIX = "/materials/";
    private static final String DOWNLOAD_SUFFIX = "/download";

    private volatile ClassroomService classroomService;
    private volatile MaterialFileStorage fileStorage;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = SessionSecurity.currentUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String path = SessionSecurity.requestPath(request);
        if (!path.startsWith(MATERIAL_PREFIX)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String remainder = path.substring(MATERIAL_PREFIX.length());
        boolean download = remainder.endsWith(DOWNLOAD_SUFFIX);
        String idPart = download
                ? remainder.substring(0, remainder.length() - DOWNLOAD_SUFFIX.length())
                : remainder;
        if (idPart.isEmpty() || idPart.indexOf('/') >= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long materialId;
        try {
            materialId = Long.parseLong(idPart);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (materialId <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        MaterialAccess access;
        try {
            access = classroomService().openMaterial(user.id(), user.role(), materialId);
        } catch (ClassroomAccessException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (RuntimeException exception) {
            renderFailure(request, response, pageFor(user), exception);
            return;
        }

        if (download) {
            download(response, access.material());
            return;
        }
        showMaterial(request, response, user, access);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void showMaterial(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser user,
            MaterialAccess access) throws ServletException, IOException {
        Page page = pageFor(user);
        applyPage(request, page);
        request.setAttribute("materialAccess", access);
        request.setAttribute("classroom", access.classroom());
        request.setAttribute("material", access.material());
        request.setAttribute("backUrl", workspaceUrl(request, user, access.material().getClassId()));
        render(request, response, page);
    }

    private void download(HttpServletResponse response, Material material) throws IOException {
        if (!material.isFile() || material.getStoredName() == null
                || material.getStoredName().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file = fileStorage().resolve(material.getStoredName());
        if (!Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ResponseSecurity.noStore(response);
        // Re-derive the response type from the allowlisted extension. Older records may
        // contain a MIME type supplied by the browser before upload types were hardened.
        response.setContentType(MaterialFileStorage.mimeTypeForExtension(material.getFileName()));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Disposition", attachmentHeader(material.getFileName()));
        long size = Files.size(file);
        if (size <= Integer.MAX_VALUE) {
            response.setContentLengthLong(size);
        }
        Files.copy(file, response.getOutputStream());
    }

    private static String attachmentHeader(String fileName) {
        String safeName = fileName == null || fileName.isBlank() ? "material" : fileName;
        String ascii = safeName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_").replace("\\", "_");
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    private static Page pageFor(SessionUser user) {
        if (user.role() == UserRole.TEACHER) {
            return new Page(
                    "/WEB-INF/views/materials/view.jsp",
                    "materials",
                    "Teacher workspace",
                    "Class material",
                    "Review the material and share it with your enrolled students.");
        }
        if (user.role() == UserRole.ADMIN) {
            return new Page(
                    "/WEB-INF/views/materials/view.jsp",
                    "classes",
                    "Admin workspace",
                    "Class material",
                    "Inspect the material shared in this class.");
        }
        return new Page(
                "/WEB-INF/views/materials/view.jsp",
                "learning",
                "Student workspace",
                "Class material",
                "Open the link, read the notes or download the file your teacher shared.");
    }

    private static String workspaceUrl(HttpServletRequest request, SessionUser user, long classId) {
        String prefix = switch (user.role()) {
            case TEACHER -> "/teacher/classes/";
            case ADMIN -> "/admin/classes/";
            default -> "/student/classes/";
        };
        return request.getContextPath() + prefix + classId + "?tab=materials";
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

    private MaterialFileStorage fileStorage() {
        MaterialFileStorage storage = fileStorage;
        if (storage == null) {
            synchronized (this) {
                storage = fileStorage;
                if (storage == null) {
                    storage = new MaterialFileStorage();
                    fileStorage = storage;
                }
            }
        }
        return storage;
    }
}
