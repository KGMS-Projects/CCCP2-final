package com.syos.server.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.syos.common.JsonProtocol;
import com.syos.common.Request;
import com.syos.common.Response;
import com.syos.server.ClientRegistry;
import com.syos.server.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Servlet for the Online Store web application.
 * Handles HTTP requests from the browser and routes them through
 * the same RequestDispatcher used by the TCP socket server.
 *
 * This ensures the same concurrency mechanisms (ReentrantLock, synchronized)
 * protect the shared data, whether the request comes from the Swing POS
 * client or the web browser.
 *
 * Endpoints:
 *   GET  /api/products         - List all products with online availability
 *   GET  /api/inventory        - Get inventory levels
 *   POST /api/login            - Authenticate user
 *   POST /api/register         - Register new user
 *   POST /api/checkout         - Process online sale
 */
public class ApiServlet extends HttpServlet {

    private final RequestDispatcher dispatcher;
    private final ClientRegistry clientRegistry;

    public ApiServlet(RequestDispatcher dispatcher, ClientRegistry clientRegistry) {
        this.dispatcher = dispatcher;
        this.clientRegistry = clientRegistry;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        if (path == null) path = "/";

        try {
            switch (path) {
                case "/products":
                    handleGetProducts(resp);
                    break;
                case "/inventory":
                    handleGetInventory(resp);
                    break;
                default:
                    sendError(resp, 404, "Not found: " + path);
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String path = req.getPathInfo();
        if (path == null) path = "/";

        // Read request body
        String body = readBody(req);

        try {
            switch (path) {
                case "/login":
                    handleLogin(body, resp);
                    break;
                case "/register":
                    handleRegister(body, resp);
                    break;
                case "/checkout":
                    handleCheckout(body, resp);
                    break;
                default:
                    sendError(resp, 404, "Not found: " + path);
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setCorsHeaders(resp);
        resp.setStatus(200);
    }

    // ==================== Handlers ====================

    private void handleGetProducts(HttpServletResponse resp) throws IOException {
        // Get products
        Request productsReq = new Request("getProducts", null);
        Response productsResp = dispatcher.dispatch(productsReq, null);

        // Get inventory for online quantities
        Request invReq = new Request("getInventoryAll", null);
        Response invResp = dispatcher.dispatch(invReq, null);

        if (productsResp.isError()) {
            sendError(resp, 500, productsResp.getError());
            return;
        }

        // Merge product data with online availability
        JsonArray products = productsResp.getResult().getAsJsonArray();
        JsonArray inventory = invResp.getResult().getAsJsonArray();

        // Build a map of productCode -> onlineQuantity
        Map<String, Integer> onlineQtyMap = new HashMap<>();
        for (JsonElement invElem : inventory) {
            JsonObject inv = invElem.getAsJsonObject();
            String code = inv.get("productCode").getAsString();
            int onlineQty = inv.get("onlineQuantity").getAsInt();
            onlineQtyMap.put(code, onlineQty);
        }

        // Add onlineQuantity to each product
        JsonArray result = new JsonArray();
        for (JsonElement prodElem : products) {
            JsonObject prod = prodElem.getAsJsonObject();
            String code = prod.get("code").getAsString();
            prod.addProperty("onlineQuantity", onlineQtyMap.getOrDefault(code, 0));
            result.add(prod);
        }

        sendJson(resp, result);
    }

    private void handleGetInventory(HttpServletResponse resp) throws IOException {
        Request req = new Request("getInventoryAll", null);
        Response response = dispatcher.dispatch(req, null);
        if (response.isError()) {
            sendError(resp, 500, response.getError());
        } else {
            sendJson(resp, response.getResult());
        }
    }

    private void handleLogin(String body, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        Map<String, Object> params = new HashMap<>();
        params.put("email", json.get("email").getAsString());
        params.put("password", json.get("password").getAsString());

        Request req = new Request("authenticateUser", params);
        Response response = dispatcher.dispatch(req, null);

        if (response.isError()) {
            sendError(resp, 401, response.getError());
        } else {
            sendJson(resp, response.getResult());
        }
    }

    private void handleRegister(String body, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        Map<String, Object> params = new HashMap<>();
        params.put("name", json.get("name").getAsString());
        params.put("email", json.get("email").getAsString());
        params.put("password", json.get("password").getAsString());
        params.put("address", json.has("address") ? json.get("address").getAsString() : "");

        Request req = new Request("registerUser", params);
        Response response = dispatcher.dispatch(req, null);

        if (response.isError()) {
            sendError(resp, 400, response.getError());
        } else {
            sendJson(resp, response.getResult());
        }
    }

    private void handleCheckout(String body, HttpServletResponse resp) throws IOException {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        Map<String, Object> params = new HashMap<>();
        // Convert JsonArray items to List<Map>
        JsonArray itemsArr = json.getAsJsonArray("items");
        java.util.List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (JsonElement itemElem : itemsArr) {
            JsonObject item = itemElem.getAsJsonObject();
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productCode", item.get("productCode").getAsString());
            itemMap.put("quantity", item.get("quantity").getAsInt());
            items.add(itemMap);
        }
        params.put("items", items);
        params.put("cashTendered", json.get("totalAmount").getAsDouble());
        params.put("transactionType", "ONLINE");
        params.put("customerId", json.has("customerId") ? json.get("customerId").getAsString() : null);

        Request req = new Request("processSale", params);
        Response response = dispatcher.dispatch(req, null);

        if (response.isError()) {
            sendError(resp, 400, response.getError());
        } else {
            // Broadcast to Swing POS clients that an online sale happened
            clientRegistry.broadcastNotification("SALE_COMPLETED", null);
            clientRegistry.broadcastNotification("INVENTORY_CHANGED", null);
            sendJson(resp, response.getResult());
        }
    }

    // ==================== Helpers ====================

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpServletResponse resp, JsonElement data) throws IOException {
        resp.setStatus(200);
        resp.getWriter().write(JsonProtocol.getGson().toJson(data));
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        resp.getWriter().write(error.toString());
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
