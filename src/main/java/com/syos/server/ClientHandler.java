package com.syos.server;

import com.syos.common.JsonProtocol;
import com.syos.common.Request;
import com.syos.common.Response;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles a single client connection on the server side.
 * Implements Runnable to be executed in the server's thread pool.
 * 
 * Each client gets two threads:
 * 1. Reader thread (this Runnable): reads requests from socket, enqueues them
 * 2. Writer thread: takes responses from responseQueue, sends to client
 * 
 * Concurrency mechanisms:
 * - Separate reader/writer threads allow non-blocking request/response flow
 * - LinkedBlockingQueue for response queuing (thread-safe, blocking)
 * - Supports rapid async requests from test clients
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RequestQueue requestQueue;
    private final ClientRegistry clientRegistry;
    private final RequestDispatcher dispatcher;

    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = true;
    private final String clientId;

    /** Queue for outgoing messages (responses + notifications) */
    private final BlockingQueue<String> outgoingQueue = new LinkedBlockingQueue<>();

    public ClientHandler(Socket socket, RequestQueue requestQueue,
                         ClientRegistry clientRegistry, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.requestQueue = requestQueue;
        this.clientRegistry = clientRegistry;
        this.dispatcher = dispatcher;
        this.clientId = socket.getRemoteSocketAddress().toString();
    }

    @Override
    public void run() {
        try {
            reader = JsonProtocol.createReader(socket);
            writer = JsonProtocol.createWriter(socket);

            // Register this client for broadcasts
            clientRegistry.register(this);

            // Start the writer thread for sending responses/notifications
            Thread writerThread = new Thread(this::writerLoop, "Writer-" + clientId);
            writerThread.setDaemon(true);
            writerThread.start();

            System.out.println("[CLIENT] Connected: " + clientId);

            // Reader loop: read requests and enqueue them
            String line;
            while (connected && (line = JsonProtocol.readMessage(reader)) != null) {
                try {
                    Request request = JsonProtocol.parseRequest(line);
                    System.out.println("[CLIENT " + clientId + "] Request: " + request.getMethod());

                    // Create queued request and enqueue it
                    RequestQueue.QueuedRequest queuedRequest =
                            new RequestQueue.QueuedRequest(request, this);
                    requestQueue.enqueue(queuedRequest);

                } catch (Exception e) {
                    System.err.println("[CLIENT " + clientId + "] Parse error: " + e.getMessage());
                    sendMessage(JsonProtocol.toJson(
                            Response.error(null, "Invalid request: " + e.getMessage())));
                }
            }

        } catch (SocketException e) {
            System.out.println("[CLIENT] Disconnected: " + clientId);
        } catch (Exception e) {
            System.err.println("[CLIENT] Error with " + clientId + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Writer loop: takes messages from the outgoing queue and sends them.
     * Runs on a separate thread to allow non-blocking writes.
     */
    private void writerLoop() {
        try {
            while (connected) {
                String message = outgoingQueue.take(); // Blocks until a message is available
                if (writer != null) {
                    JsonProtocol.sendMessage(writer, message);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (connected) {
                System.err.println("[WRITER " + clientId + "] Error: " + e.getMessage());
            }
        }
    }

    /**
     * Queues a message to be sent to this client.
     * Thread-safe - can be called from any thread (dispatcher, broadcaster, etc.)
     */
    public void sendMessage(String json) {
        if (connected) {
            outgoingQueue.offer(json);
        }
    }

    /**
     * Sends a Response directly to this client's outgoing queue.
     */
    public void sendResponse(Response response) {
        sendMessage(JsonProtocol.toJson(response));
    }

    /** Disconnect this client cleanly */
    private void disconnect() {
        connected = false;
        clientRegistry.unregister(this);
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
        System.out.println("[CLIENT] Cleaned up: " + clientId);
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isConnected() {
        return connected;
    }
}
