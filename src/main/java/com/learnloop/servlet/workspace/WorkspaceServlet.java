package com.learnloop.servlet.workspace;

import com.learnloop.dao.Database;
import com.learnloop.security.ResponseSecurity;
import com.learnloop.service.DashboardService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class WorkspaceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(WorkspaceServlet.class.getName());

    private volatile DashboardService dashboardService;

    protected final DashboardService dashboardService() {
        DashboardService service = dashboardService;
        if (service == null) {
            synchronized (this) {
                service = dashboardService;
                if (service == null) {
                    service = new DashboardService(Database.fromConfiguration());
                    dashboardService = service;
                }
            }
        }
        return service;
    }

    protected final void applyPage(HttpServletRequest request, Page page) {
        request.setAttribute("pageTitle", "LearnLoop - " + page.title());
        request.setAttribute("workspaceNav", page.nav());
        request.setAttribute("workspaceEyebrow", page.eyebrow());
        request.setAttribute("workspaceTitle", page.title());
        request.setAttribute("workspaceLead", page.lead());
    }

    protected final void render(HttpServletRequest request, HttpServletResponse response, Page page)
            throws ServletException, IOException {
        ResponseSecurity.noStore(response);
        request.getRequestDispatcher(page.view()).forward(request, response);
    }

    protected final void renderFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            Page page,
            RuntimeException failure) throws ServletException, IOException {
        LOGGER.log(Level.SEVERE, "Workspace data could not be loaded for " + page.view(), failure);
        ResponseSecurity.noStore(response);
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        request.setAttribute(
                "dashboardError",
                "Dashboard data is temporarily unavailable. The database could not be reached. Please try again.");
        request.getRequestDispatcher(page.view()).forward(request, response);
    }

    public record Page(String view, String nav, String eyebrow, String title, String lead) {
    }
}
