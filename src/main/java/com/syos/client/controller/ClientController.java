package com.syos.client.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.syos.client.ServerConnection;
import com.syos.common.Response;

import java.util.*;

/**
 * Client-side controller that provides high-level business operations.
 * Acts as a proxy to the server — all data access goes through the network.
 * 
 * Clean Architecture role: Interface Adapter on the client side.
 * This controller translates GUI actions into server requests and
 * server responses into GUI-friendly data structures.
 * 
 * The client has NO direct database access. All operations go through
 * the server via TCP sockets, maintaining the 3-tier separation.
 */
public class ClientController {

    private final ServerConnection connection;

    public ClientController(ServerConnection connection) {
        this.connection = connection;
    }

    // ==================== Product Operations ====================

    public List<Map<String, Object>> getProducts() throws Exception {
        Response response = connection.sendRequest("getProducts", null);
        checkError(response);
        return jsonArrayToList(response.getResult().getAsJsonArray());
    }

    public Map<String, Object> addProduct(String code, String name, String unit,
                                           double price, double discountPercentage,
                                           int initialStock, String expiryDate) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("name", name);
        params.put("unit", unit);
        params.put("price", price);
        params.put("discountPercentage", discountPercentage);
        params.put("initialStock", initialStock);
        params.put("expiryDate", expiryDate);

        Response response = connection.sendRequest("addProduct", params);
        checkError(response);
        return jsonObjectToMap(response.getResult().getAsJsonObject());
    }

    public boolean productExists(String code) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        Response response = connection.sendRequest("productExists", params);
        checkError(response);
        return response.getResult().getAsBoolean();
    }

    // ==================== Inventory Operations ====================

    public List<Map<String, Object>> getInventoryAll() throws Exception {
        Response response = connection.sendRequest("getInventoryAll", null);
        checkError(response);
        return jsonArrayToList(response.getResult().getAsJsonArray());
    }

    public Map<String, Object> getInventory(String productCode) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("productCode", productCode);
        Response response = connection.sendRequest("getInventory", params);
        checkError(response);
        return jsonObjectToMap(response.getResult().getAsJsonObject());
    }

    // ==================== Sale Operations ====================

    public Map<String, Object> processSale(List<Map<String, Object>> items,
                                            double cashTendered,
                                            String transactionType,
                                            String customerId) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("items", items);
        params.put("cashTendered", cashTendered);
        params.put("transactionType", transactionType);
        if (customerId != null) params.put("customerId", customerId);

        Response response = connection.sendRequest("processSale", params);
        checkError(response);
        return jsonObjectToMap(response.getResult().getAsJsonObject());
    }

    // ==================== Stock Operations ====================

    public Map<String, Object> addStockBatch(String productCode, int quantity,
                                              String expiryDate) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("productCode", productCode);
        params.put("quantity", quantity);
        params.put("expiryDate", expiryDate);

        Response response = connection.sendRequest("addStockBatch", params);
        checkError(response);
        return jsonObjectToMap(response.getResult().getAsJsonObject());
    }

    public List<Map<String, Object>> getStockBatchesAll() throws Exception {
        Response response = connection.sendRequest("getStockBatchesAll", null);
        checkError(response);
        return jsonArrayToList(response.getResult().getAsJsonArray());
    }

    public boolean transferStock(String productCode, int quantity, String transferType) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("productCode", productCode);
        params.put("quantity", quantity);
        params.put("transferType", transferType);

        Response response = connection.sendRequest("transferStock", params);
        checkError(response);
        return true;
    }

    // ==================== User Operations ====================

    public Map<String, Object> authenticateUser(String email, String password) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        Response response = connection.sendRequest("authenticateUser", params);
        checkError(response);
        return jsonObjectToMap(response.getResult().getAsJsonObject());
    }

    public boolean registerUser(String name, String email, String password, String address) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("email", email);
        params.put("password", password);
        params.put("address", address);

        Response response = connection.sendRequest("registerUser", params);
        checkError(response);
        return true;
    }

    // ==================== Report Operations ====================

    public String generateReport(String reportType, String date, String transactionType) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", reportType);
        if (date != null) params.put("date", date);
        if (transactionType != null) params.put("transactionType", transactionType);

        Response response = connection.sendRequest("generateReport", params);
        checkError(response);
        return response.getResult().getAsString();
    }

    // ==================== Server Info ====================

    public boolean ping() {
        try {
            Response response = connection.sendRequest("ping", null);
            return !response.isError();
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Helper Methods ====================

    private void checkError(Response response) throws Exception {
        if (response.isError()) {
            throw new Exception(response.getError());
        }
    }

    private List<Map<String, Object>> jsonArrayToList(JsonArray array) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JsonElement elem : array) {
            if (elem.isJsonObject()) {
                list.add(jsonObjectToMap(elem.getAsJsonObject()));
            }
        }
        return list;
    }

    private Map<String, Object> jsonObjectToMap(JsonObject obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : obj.keySet()) {
            JsonElement val = obj.get(key);
            if (val.isJsonNull()) {
                map.put(key, null);
            } else if (val.isJsonPrimitive()) {
                if (val.getAsJsonPrimitive().isNumber()) {
                    map.put(key, val.getAsNumber());
                } else if (val.getAsJsonPrimitive().isBoolean()) {
                    map.put(key, val.getAsBoolean());
                } else {
                    map.put(key, val.getAsString());
                }
            } else if (val.isJsonArray()) {
                map.put(key, jsonArrayToList(val.getAsJsonArray()));
            } else if (val.isJsonObject()) {
                map.put(key, jsonObjectToMap(val.getAsJsonObject()));
            }
        }
        return map;
    }
}
