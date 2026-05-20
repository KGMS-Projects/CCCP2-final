package com.syos.server.web;

import com.syos.server.ClientRegistry;
import com.syos.server.RequestDispatcher;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.Wrapper;

import java.io.File;

/**
 * Embedded Tomcat server for the Online Store web application.
 * Runs alongside the TCP socket server, sharing the same RequestDispatcher
 * and concurrency mechanisms (ReentrantLock, etc.).
 *
 * Architecture:
 * - Serves static HTML/CSS/JS files for the customer-facing online store
 * - Provides REST API endpoints via ApiServlet
 * - Reuses the same RequestDispatcher → same Use Cases → same Database
 * - Thread safety ensured by the same ReentrantLock in RequestDispatcher
 */
public class WebAppServer {

    private final Tomcat tomcat;
    private final int port;

    public WebAppServer(int port, RequestDispatcher dispatcher, ClientRegistry clientRegistry) {
        this.port = port;
        this.tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Force connector initialization

        // Create context with a temporary working directory
        String tempDir = System.getProperty("java.io.tmpdir");
        Context ctx = tomcat.addContext("", new File(tempDir).getAbsolutePath());

        // API Servlet — handles all /api/* requests
        Wrapper apiWrapper = Tomcat.addServlet(ctx, "api", new ApiServlet(dispatcher, clientRegistry));
        apiWrapper.setLoadOnStartup(1);
        ctx.addServletMappingDecoded("/api/*", "api");

        // Static File Servlet — serves HTML/CSS/JS for the online store
        String webappPath = new File("src/main/webapp").getAbsolutePath();
        Wrapper staticWrapper = Tomcat.addServlet(ctx, "static", new StaticFileServlet(webappPath));
        staticWrapper.setLoadOnStartup(1);
        ctx.addServletMappingDecoded("/", "static");
        ctx.addServletMappingDecoded("/*", "static");
    }

    /**
     * Starts the embedded Tomcat server.
     * Called from SyosServer after the TCP socket server is initialized.
     */
    public void start() throws Exception {
        tomcat.start();
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("  Online Store running at:");
        System.out.println("  http://localhost:" + port);
        System.out.println("══════════════════════════════════════════\n");
    }

    /** Stops the Tomcat server gracefully */
    public void stop() {
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception e) {
            System.err.println("[WEBSERVER] Error stopping: " + e.getMessage());
        }
    }
}
