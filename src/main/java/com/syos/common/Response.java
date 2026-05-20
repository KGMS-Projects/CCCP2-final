package com.syos.common;

import com.google.gson.JsonElement;

/**
 * Represents a JSON-RPC style response sent from server to client.
 * Can also represent a push notification (when id is null).
 */
public class Response {
    private String id;
    private JsonElement result;
    private String error;
    private String notificationType; // For push notifications

    public Response() {}

    /** Success response */
    public static Response success(String id, JsonElement result) {
        Response r = new Response();
        r.id = id;
        r.result = result;
        return r;
    }

    /** Error response */
    public static Response error(String id, String errorMessage) {
        Response r = new Response();
        r.id = id;
        r.error = errorMessage;
        return r;
    }

    /** Push notification (no request id) */
    public static Response notification(String type, JsonElement data) {
        Response r = new Response();
        r.id = null;
        r.notificationType = type;
        r.result = data;
        return r;
    }

    public String getId() { return id; }
    public JsonElement getResult() { return result; }
    public String getError() { return error; }
    public String getNotificationType() { return notificationType; }
    public boolean isError() { return error != null && !error.isEmpty(); }
    public boolean isNotification() { return notificationType != null; }

    @Override
    public String toString() {
        if (isNotification()) return "Notification{type='" + notificationType + "'}";
        if (isError()) return "Response{id='" + id + "', error='" + error + "'}";
        return "Response{id='" + id + "', result=" + result + "}";
    }
}
