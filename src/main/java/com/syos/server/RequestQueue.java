package com.syos.server;

import com.syos.common.Request;
import com.syos.common.Response;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe request queue for the server.
 * Implements the Producer-Consumer pattern using a LinkedBlockingQueue.
 * 
 * Concurrency mechanisms used:
 * - LinkedBlockingQueue: Thread-safe bounded/unbounded queue with blocking operations
 * - AtomicInteger: Lock-free thread-safe counter for queue statistics
 * - AtomicLong: Lock-free thread-safe counter for total processed count
 * 
 * When multiple clients send requests simultaneously (especially from fast
 * automatic test clients), requests are queued here and processed in order
 * by the worker thread pool. This ensures the server never drops requests,
 * even under heavy load.
 */
public class RequestQueue {

    /** Wrapper that pairs a request with its response callback */
    public static class QueuedRequest {
        private final Request request;
        private final ClientHandler clientHandler;
        private final CompletableFuture<Response> responseFuture;
        private final long enqueueTimeNanos;

        public QueuedRequest(Request request, ClientHandler clientHandler) {
            this.request = request;
            this.clientHandler = clientHandler;
            this.responseFuture = new CompletableFuture<>();
            this.enqueueTimeNanos = System.nanoTime();
        }

        public Request getRequest() { return request; }
        public ClientHandler getClientHandler() { return clientHandler; }
        public CompletableFuture<Response> getResponseFuture() { return responseFuture; }
        public long getEnqueueTimeNanos() { return enqueueTimeNanos; }
    }

    private final LinkedBlockingQueue<QueuedRequest> queue;
    private final AtomicInteger peakQueueSize;
    private final AtomicLong totalEnqueued;
    private final AtomicLong totalProcessed;

    public RequestQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.peakQueueSize = new AtomicInteger(0);
        this.totalEnqueued = new AtomicLong(0);
        this.totalProcessed = new AtomicLong(0);
    }

    public RequestQueue() {
        this(1000); // Default capacity
    }

    /**
     * Enqueues a request for processing. Blocks if the queue is full.
     * Called by ClientHandler threads (producers).
     */
    public void enqueue(QueuedRequest queuedRequest) throws InterruptedException {
        queue.put(queuedRequest);
        totalEnqueued.incrementAndGet();
        int currentSize = queue.size();
        peakQueueSize.updateAndGet(peak -> Math.max(peak, currentSize));
        System.out.println("[QUEUE] Enqueued request: " + queuedRequest.getRequest().getMethod()
                + " | Queue depth: " + currentSize);
    }

    /**
     * Takes the next request from the queue. Blocks if empty.
     * Called by worker threads (consumers).
     */
    public QueuedRequest take() throws InterruptedException {
        QueuedRequest qr = queue.take();
        totalProcessed.incrementAndGet();
        return qr;
    }

    /** Current number of requests waiting in the queue */
    public int size() { return queue.size(); }

    /** Peak queue depth observed */
    public int getPeakQueueSize() { return peakQueueSize.get(); }

    /** Total requests enqueued since server start */
    public long getTotalEnqueued() { return totalEnqueued.get(); }

    /** Total requests processed since server start */
    public long getTotalProcessed() { return totalProcessed.get(); }

    /** Get queue statistics as a formatted string */
    public String getStats() {
        return String.format("Queue Stats: current=%d, peak=%d, enqueued=%d, processed=%d",
                size(), getPeakQueueSize(), getTotalEnqueued(), getTotalProcessed());
    }
}
