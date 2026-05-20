package com.syos.server;

import com.syos.common.JsonProtocol;
import com.syos.common.Response;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry of all connected client handlers.
 * Used for broadcasting push notifications (e.g., inventory changes)
 * to all connected clients in real-time.
 * 
 * Concurrency mechanism: CopyOnWriteArrayList
 * - Optimized for scenarios with frequent reads (broadcasts) and infrequent writes (connect/disconnect)
 * - Iterators never throw ConcurrentModificationException
 * - Thread-safe without explicit synchronization for iteration
 */
public class ClientRegistry {

    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /** Register a new client connection */
    public void register(ClientHandler handler) {
        clients.add(handler);
        System.out.println("[REGISTRY] Client connected. Total clients: " + clients.size());
    }

    /** Unregister a disconnected client */
    public void unregister(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("[REGISTRY] Client disconnected. Total clients: " + clients.size());
    }

    /** Get the number of connected clients */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Broadcast a push notification to ALL connected clients.
     * Called after data-modifying operations (sales, stock changes, etc.)
     * so that all clients can refresh their displays in real-time.
     * 
     * Uses CopyOnWriteArrayList iteration - safe even if clients
     * connect/disconnect during broadcast.
     */
    public void broadcastNotification(String notificationType, Object data) {
        Response notification = Response.notification(
                notificationType,
                JsonProtocol.toJsonElement(data)
        );
        String json = JsonProtocol.toJson(notification);

        System.out.println("[BROADCAST] Sending " + notificationType + " to " + clients.size() + " clients");

        for (ClientHandler client : clients) {
            try {
                client.sendMessage(json);
            } catch (Exception e) {
                System.err.println("[BROADCAST] Failed to notify client: " + e.getMessage());
            }
        }
    }

    /**
     * Broadcast to all clients EXCEPT the one who triggered the change.
     * Prevents the originating client from receiving its own notification.
     */
    public void broadcastNotificationExcept(String notificationType, Object data, ClientHandler except) {
        Response notification = Response.notification(
                notificationType,
                JsonProtocol.toJsonElement(data)
        );
        String json = JsonProtocol.toJson(notification);

        for (ClientHandler client : clients) {
            if (client != except) {
                try {
                    client.sendMessage(json);
                } catch (Exception e) {
                    System.err.println("[BROADCAST] Failed to notify client: " + e.getMessage());
                }
            }
        }
    }
}
