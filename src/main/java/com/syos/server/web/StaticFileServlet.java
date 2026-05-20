package com.syos.server.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.nio.file.*;

/**
 * Servlet that serves static files (HTML, CSS, JS) for the online store.
 * Reads files from the src/main/webapp directory.
 */
public class StaticFileServlet extends HttpServlet {

    private final String basePath;

    public StaticFileServlet(String basePath) {
        this.basePath = basePath;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getRequestURI();

        // Skip API paths
        if (path.startsWith("/api")) return;

        // Default to index.html
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File file = new File(basePath + path);

        if (!file.exists() || file.isDirectory()) {
            // Try index.html for directory requests
            file = new File(basePath + "/index.html");
            if (!file.exists()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("404 Not Found");
                return;
            }
        }

        // Set content type based on extension
        String contentType = getContentType(path);
        resp.setContentType(contentType);
        resp.setCharacterEncoding("UTF-8");

        // Stream the file
        Files.copy(file.toPath(), resp.getOutputStream());
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
