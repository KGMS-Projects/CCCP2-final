package com.syos.client;

import com.syos.common.JsonProtocol;
import com.syos.common.Request;
import com.syos.common.Response;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Concurrency mechanisms:
 * - ConcurrentHashMap: Thread-safe storage for pending request futures.
 *   Allows multiple threads to send requests and receive responses simultaneously.
 * - CompletableFuture: Enables non-blocking async request-response matching.
 *   The listener thread completes futures when responses arrive.
 * - Listener Thread: Dedicated thread that continuously reads from the socket,
 *   matching responses to pending requests and dispatching notifications.
 * - CopyOnWriteArrayList: Thread-safe list for notification listeners.
 */
public class ServerConnection {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;

    /** Pending requests waiting for responses, keyed by request ID */
    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests
            = new ConcurrentHashMap<>();

    /** Listeners for push notifications from the server */
    private final CopyOnWriteArrayList<Consumer<Response>> notificationListeners
            = new CopyOnWriteArrayList<>();

    private Thread listenerThread;

    public ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Establishes connection to the server.
     * Starts a dedicated listener thread for receiving messages.
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = JsonProtocol.createReader(socket);
        writer = JsonProtocol.createWriter(socket);
        connected = true;

        // Start listener thread for incoming messages
        listenerThread = new Thread(this::listenerLoop, "ServerListener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        System.out.println("[CONNECTION] Connected to server at " + host + ":" + port);
    }

    /**
     * Listener loop: continuously reads messages from the server.
     * - Responses (with ID): matched to pending CompletableFuture
     * - Notifications (no ID): dispatched to registered listeners
     */
    private void listenerLoop() {
        try {
            while (connected) {
                String line = JsonProtocol.readMessage(reader);
                if (line == null) {
                    System.out.println("[CONNECTION] Server closed connection");
                    break;
                }

                try {
                    Response response = JsonProtocol.parseResponse(line);

                    if (response.isNotification()) {
                        // Push notification from server
                        for (Consumer<Response> listener : notificationListeners) {
                            try {
                                listener.accept(response);
                            } catch (Exception e) {
                                System.err.println("[CONNECTION] Notification listener error: " + e.getMessage());
                            }
                        }
                    } else if (response.getId() != null) {
                        // Response to a pending request
                        CompletableFuture<Response> future = pendingRequests.remove(response.getId());
                        if (future != null) {
                            future.complete(response);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CONNECTION] Parse error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (connected) {
                System.err.println("[CONNECTION] Listener error: " + e.getMessage());
            }
        } finally {
            connected = false;
            // Complete all pending requests with error
            for (CompletableFuture<Response> future : pendingRequests.values()) {
                future.completeExceptionally(new IOException("Connection lost"));
            }
            pendingRequests.clear();
        }
    }

    /**
     * Sends a request to the server asynchronously.
     * Returns a CompletableFuture that will be completed when the response arrives.
     * 
     * Thread-safe: multiple threads can call this simultaneously.
     */
    public CompletableFuture<Response> sendRequestAsync(String method, Map<String, Object> params) {
        if (!connected) {
            CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(new IOException("Not connected to server"));
            return future;
        }

        Request request = new Request(method, params);
        CompletableFuture<Response> future = new CompletableFuture<>();

        // Register the pending request
        pendingRequests.put(request.getId(), future);

        // Send the request (synchronized in JsonProtocol.sendMessage)
        String json = JsonProtocol.toJson(request);
        JsonProtocol.sendMessage(writer, json);

        // Add timeout
        future.orTimeout(30, TimeUnit.SECONDS);

        return future;
    }

    /**
     * Sends a request and blocks until the response is received.
     * Convenience method for synchronous usage.
     */
    public Response sendRequest(String method, Map<String, Object> params) throws Exception {
        return sendRequestAsync(method, params).get(30, TimeUnit.SECONDS);
    }

    /** Register a listener for server push notifications */
    public void addNotificationListener(Consumer<Response> listener) {
        notificationListeners.add(listener);
    }

    /** Remove a notification listener */
    public void removeNotificationListener(Consumer<Response> listener) {
        notificationListeners.remove(listener);
    }

    /** Check if connected to server */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    /** Disconnect from the server */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        System.out.println("[CONNECTION] Disconnected");
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}
