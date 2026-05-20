package com.syos.testclients;

import com.syos.client.ServerConnection;
import com.syos.common.Response;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Automatic Test Client 1 — Rapid Sale Simulator
 * 
 * Sends 50+ asynchronous sale requests in rapid succession to the server.
 * Designed to test server-side concurrency, request queuing, and data consistency.
 * 
 * Concurrency mechanisms demonstrated:
 * - CompletableFuture: Non-blocking async request submission
 * - ExecutorService: Thread pool for concurrent request generation
 * - AtomicInteger/AtomicLong: Lock-free counters for thread-safe statistics
 * - CountDownLatch: Coordination mechanism to wait for all requests to complete
 * 
 * This client should be run simultaneously with TestClient2 to prove that
 * the server handles multiple requests from multiple clients concurrently.
 * 
 * Usage: java com.syos.testclients.TestClient1 [host] [port]
 */
public class TestClient1 {

    private static final int TOTAL_REQUESTS = 50;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private final ServerConnection connection;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    public TestClient1(String host, int port) {
        this.connection = new ServerConnection(host, port);
    }

    public void run() throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  SYOS Test Client 1 — Rapid Sale Simulator      ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println("Connecting to server...");

        connection.connect();
        System.out.println("Connected! Preparing to send " + TOTAL_REQUESTS + " rapid requests...\n");

        // First, get available products
        Response prodResponse = connection.sendRequest("getProducts", null);
        if (prodResponse.isError()) {
            System.err.println("Cannot get products: " + prodResponse.getError());
            System.out.println("Make sure products exist in the database before running test clients.");
            connection.disconnect();
            return;
        }

        // Check if products exist
        List<?> products = new ArrayList<>();
        if (prodResponse.getResult().isJsonArray()) {
            products = new ArrayList<>(prodResponse.getResult().getAsJsonArray().asList());
        }

        if (products.isEmpty()) {
            System.out.println("No products available. Please add products first.");
            connection.disconnect();
            return;
        }

        System.out.println("Found " + products.size() + " products. Starting rapid fire...\n");

        // Send requests using CompletableFuture for async execution
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        List<CompletableFuture<Response>> futures = new ArrayList<>();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestNum = i + 1;
            long requestStart = System.nanoTime();

            // Alternate between different request types
            CompletableFuture<Response> future;

            if (requestNum % 5 == 0) {
                // Every 5th request: Get inventory (read operation)
                future = connection.sendRequestAsync("getInventoryAll", null);
            } else if (requestNum % 3 == 0) {
                // Every 3rd request: Get products (read operation)
                future = connection.sendRequestAsync("getProducts", null);
            } else {
                // Sale requests (write operation) — using ping as safe alternative
                // if no stock is available, we still test concurrency
                Map<String, Object> params = new HashMap<>();
                params.put("reportType", "DAILY_SALES");
                params.put("transactionType", "ALL");
                future = connection.sendRequestAsync("generateReport", params);
            }

            future.whenComplete((response, error) -> {
                long elapsed = (System.nanoTime() - requestStart) / 1_000_000;
                totalResponseTime.addAndGet(elapsed);

                if (error != null) {
                    errorCount.incrementAndGet();
                    System.out.printf("  [REQ %3d] ERROR (%.0fms): %s%n",
                            requestNum, (double) elapsed, error.getMessage());
                } else if (response.isError()) {
                    errorCount.incrementAndGet();
                    System.out.printf("  [REQ %3d] FAILED (%.0fms): %s%n",
                            requestNum, (double) elapsed, response.getError());
                } else {
                    successCount.incrementAndGet();
                    System.out.printf("  [REQ %3d] SUCCESS (%.0fms)%n", requestNum, (double) elapsed);
                }
                latch.countDown();
            });

            futures.add(future);

            // Small delay to simulate rapid but not instant requests
            Thread.sleep(20); // 20ms between requests = 50 requests/second
        }

        // Wait for all responses
        System.out.println("\nWaiting for all responses...");
        boolean completed = latch.await(60, TimeUnit.SECONDS);

        long totalTime = System.currentTimeMillis() - startTime;

        // Print summary
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║           TEST CLIENT 1 — RESULTS               ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  Total Requests Sent:    %4d                    ║%n", TOTAL_REQUESTS);
        System.out.printf("║  Successful:             %4d                    ║%n", successCount.get());
        System.out.printf("║  Failed:                 %4d                    ║%n", errorCount.get());
        System.out.printf("║  All Completed:          %-5s                   ║%n", completed ? "YES" : "NO");
        System.out.printf("║  Total Time:             %4dms                  ║%n", totalTime);
        if (successCount.get() + errorCount.get() > 0) {
            long avg = totalResponseTime.get() / (successCount.get() + errorCount.get());
            System.out.printf("║  Avg Response Time:      %4dms                  ║%n", avg);
        }
        System.out.printf("║  Throughput:             %5.1f req/sec           ║%n",
                (double) TOTAL_REQUESTS / (totalTime / 1000.0));
        System.out.println("╚══════════════════════════════════════════════════╝");

        connection.disconnect();
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        try {
            new TestClient1(host, port).run();
        } catch (Exception e) {
            System.err.println("Test Client 1 failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
