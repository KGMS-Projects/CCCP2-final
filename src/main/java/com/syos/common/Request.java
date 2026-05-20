package com.syos.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a JSON-RPC style request sent from client to server.
 * Part of the common protocol shared between client and server tiers.
 */
public class Request {
    private String id;
    private String method;
    private Map<String, Object> params;

    public Request() {
        this.id = UUID.randomUUID().toString();
        this.params = new HashMap<>();
    }

    public Request(String method, Map<String, Object> params) {
        this.id = UUID.randomUUID().toString();
        this.method = method;
        this.params = params != null ? params : new HashMap<>();
    }

    public Request(String id, String method, Map<String, Object> params) {
        this.id = id;
        this.method = method;
        this.params = params != null ? params : new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public Object getParam(String key) {
        return params.get(key);
    }

    public String getStringParam(String key) {
        Object val = params.get(key);
        return val != null ? val.toString() : null;
    }

    public int getIntParam(String key) {
        Object val = params.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return 0;
    }

    public double getDoubleParam(String key) {
        Object val = params.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) return Double.parseDouble((String) val);
        return 0.0;
    }

    @Override
    public String toString() {
        return "Request{id='" + id + "', method='" + method + "', params=" + params + "}";
    }
}
