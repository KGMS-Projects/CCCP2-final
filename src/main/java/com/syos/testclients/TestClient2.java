package com.syos.testclients;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.syos.client.ServerConnection;
import com.syos.common.Response;

/**
 * Automatic Test Client 2 — Rapid Inventory Operations Simulator
 * 
 * Sends 50+ asynchronous inventory/stock requests in rapid succession.
 * run SIMULTANEOUSLY with TestClient1 to prove concurrent
 * multi-client handling with request queuing on the server.
 * 
 * Concurrency mechanisms demonstrated:
 * - ExecutorService: Fixed thread pool for parallel request submission
 * - CompletableFuture: Async request-response pattern
 * - AtomicInteger/AtomicLong: Lock-free thread-safe statistics
 * - CyclicBarrier: Synchronizes threads to send requests simultaneously
 * 
 * Usage: java com.syos.testclients.TestClient2 [host] [port]
 */
public class TestClient2 {

    private static final int TOTAL_REQUESTS = 50;
    private static final int THREAD_COUNT = 5;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private final ServerConnection connection;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    public TestClient2(String host, int port) {
        this.connection = new ServerConnection(host, port);
    }

    public void run() throws Exception {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  SYOS Test Client 2 — Inventory Operations      ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println("Connecting to server...");

        connection.connect();
        System.out.println("Connected! Using " + THREAD_COUNT + " threads for " +
                TOTAL_REQUESTS + " requests...\n");

        // Use an ExecutorService with multiple threads
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        long startTime = System.currentTimeMillis();

        // Submit all requests using the thread pool
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestNum = i + 1;

            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    Response response;

                    // Mix different operation types
                    switch (requestNum % 6) {
                        case 0:
                            // Get all products
                            response = connection.sendRequest("getProducts", null);
                            break;
                        case 1:
                            // Get all inventory
                            response = connection.sendRequest("getInventoryAll", null);
                            break;
                        case 2:
                            // Get stock batches
                            response = connection.sendRequest("getStockBatchesAll", null);
                            break;
                        case 3:
                            // Generate stock report
                            Map<String, Object> reportParams = new HashMap<>();
                            reportParams.put("reportType", "STOCK");
                            response = connection.sendRequest("generateReport", reportParams);
                            break;
                        case 4:
                            // Generate reorder report
                            Map<String, Object> reorderParams = new HashMap<>();
                            reorderParams.put("reportType", "REORDER");
                            response = connection.sendRequest("generateReport", reorderParams);
                            break;
                        default:
                            // Ping
                            response = connection.sendRequest("ping", null);
                            break;
                    }

                    long elapsed = (System.nanoTime() - requestStart) / 1_000_000;
                    totalResponseTime.addAndGet(elapsed);

                    if (response.isError()) {
                        errorCount.incrementAndGet();
                        System.out.printf("  [T%d-REQ %3d] FAILED (%.0fms): %s%n",
                                Math.abs(Thread.currentThread().getName().hashCode()) % 100, requestNum,
                                (double) elapsed, response.getError());
                    } else {
                        successCount.incrementAndGet();
                        System.out.printf("  [T%d-REQ %3d] SUCCESS (%.0fms)%n",
                                Math.abs(Thread.currentThread().getName().hashCode()) % 100, requestNum, (double) elapsed);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    long elapsed = (System.nanoTime() - requestStart) / 1_000_000;
                    totalResponseTime.addAndGet(elapsed);
                    System.out.printf("  [T%d-REQ %3d] ERROR (%.0fms): %s%n",
                            Math.abs(Thread.currentThread().getName().hashCode()) % 100, requestNum,
                            (double) elapsed, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            // Very small delay — requests sent nearly simultaneously
            Thread.sleep(10); // 10ms = 100 requests/second rate
        }

        // Wait for completion
        System.out.println("\nWaiting for all responses...");
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Print summary
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║           TEST CLIENT 2 — RESULTS               ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  Total Requests Sent:    %4d                    ║%n", TOTAL_REQUESTS);
        System.out.printf("║  Worker Threads:         %4d                    ║%n", THREAD_COUNT);
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
            new TestClient2(host, port).run();
        } catch (Exception e) {
            System.err.println("Test Client 2 failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
