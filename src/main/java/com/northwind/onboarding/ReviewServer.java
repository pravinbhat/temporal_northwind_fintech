package com.northwind.onboarding;

import com.northwind.onboarding.model.ReviewDecision;
import com.northwind.onboarding.workflow.OnboardingWorkflow;
import com.sun.net.httpserver.HttpServer;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * HTTP endpoint for compliance review decisions.
 *
 *   POST /review/{workflowId}/approve   → sends APPROVED decision to the workflow
 *   POST /review/{workflowId}/reject    → sends REJECTED decision to the workflow
 *
 * In a production system this would be a proper REST API secured behind auth,
 * integrated with a compliance UI.
 */
public class ReviewServer {

    private static final Logger log = LoggerFactory.getLogger(ReviewServer.class);
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        log.info("Starting compliance review server on port {}...", PORT);

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/review/", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed — use POST");
                return;
            }

            String path = exchange.getRequestURI().getPath();  // e.g. /review/onboarding-CUST-123/approve
            String[] parts = path.split("/");

            if (parts.length != 4) {
                sendResponse(exchange, 400, "Invalid path — use /review/{workflowId}/{approve|reject}");
                return;
            }

            String workflowId = parts[2];
            String action = parts[3];

            ReviewDecision decision;
            if ("approve".equalsIgnoreCase(action)) {
                decision = ReviewDecision.APPROVED;
            } else if ("reject".equalsIgnoreCase(action)) {
                decision = ReviewDecision.REJECTED;
            } else {
                sendResponse(exchange, 400, "Invalid action — use 'approve' or 'reject'");
                return;
            }

            try {
                OnboardingWorkflow workflow = client.newWorkflowStub(OnboardingWorkflow.class, workflowId);
                workflow.reviewDecision(decision);

                log.info("✓ Sent {} signal to workflow {}", decision, workflowId);
                sendResponse(exchange, 200,
                        String.format("Review decision %s sent to workflow %s", decision, workflowId));

            } catch (Exception e) {
                log.error("Failed to send signal to workflow {}: {}", workflowId, e.getMessage());
                sendResponse(exchange, 500, "Failed to send signal: " + e.getMessage());
            }
        });

        server.setExecutor(null);
        server.start();

        log.info("✓ Review server started");
        log.info("  Listening on: http://localhost:{}", PORT);
        log.info("  Approve:      curl -X POST http://localhost:{}/review/{{workflowId}}/approve", PORT);
        log.info("  Reject:       curl -X POST http://localhost:{}/review/{{workflowId}}/reject", PORT);
        log.info("");
        log.info("Press Ctrl+C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down review server...");
            server.stop(0);
            service.shutdown();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
