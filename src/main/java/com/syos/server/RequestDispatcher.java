package com.syos.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.syos.common.JsonProtocol;
import com.syos.common.Request;
import com.syos.common.Response;
import com.syos.entities.*;
import com.syos.usecases.*;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.reports.*;
import com.syos.usecases.repositories.*;
import com.syos.usecases.strategies.StockSelectionStrategy;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Routes incoming requests to the appropriate use case and returns responses.
 * Acts as the Interface Adapter layer in Clean Architecture on the server side.
 * 
 * Concurrency mechanisms:
 * - ReentrantLock: Provides mutual exclusion for inventory-modifying operations
 *   (processSale, addStockBatch, transferStock) to prevent race conditions
 *   like double-selling the same stock.
 * - synchronized blocks: Used for report generation to ensure consistent reads.
 */
public class RequestDispatcher {

    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final InventoryRepository inventoryRepository;
    private final StockBatchRepository stockBatchRepository;
    private final UserRepository userRepository;

    private final ProcessSaleUseCase processSaleUseCase;
    private final AddStockBatchUseCase addStockBatchUseCase;
    private final TransferStockUseCase transferStockUseCase;
    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final InventorySubject inventorySubject;

    private final ClientRegistry clientRegistry;

    /**
     * ReentrantLock for inventory-modifying operations.
     * Ensures that concurrent sales/stock changes don't cause
     * race conditions (e.g., double-selling the last item).
     * 
     * ReentrantLock chosen over synchronized because:
     * 1. It supports tryLock() for timeout-based acquisition
     * 2. It provides fairness policy to prevent thread starvation
     * 3. It can be released in a different scope than where acquired
     */
    private final ReentrantLock inventoryLock = new ReentrantLock(true); // fair lock

    public RequestDispatcher(
            ProductRepository productRepository,
            BillRepository billRepository,
            InventoryRepository inventoryRepository,
            StockBatchRepository stockBatchRepository,
            UserRepository userRepository,
            ProcessSaleUseCase processSaleUseCase,
            AddStockBatchUseCase addStockBatchUseCase,
            TransferStockUseCase transferStockUseCase,
            RegisterUserUseCase registerUserUseCase,
            AuthenticateUserUseCase authenticateUserUseCase,
            InventorySubject inventorySubject,
            ClientRegistry clientRegistry) {
        this.productRepository = productRepository;
        this.billRepository = billRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.userRepository = userRepository;
        this.processSaleUseCase = processSaleUseCase;
        this.addStockBatchUseCase = addStockBatchUseCase;
        this.transferStockUseCase = transferStockUseCase;
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.inventorySubject = inventorySubject;
        this.clientRegistry = clientRegistry;
    }

    /**
     * Dispatches a request to the appropriate handler method.
     * This is the central routing point for all client requests.
     */
    public Response dispatch(Request request, ClientHandler sourceClient) {
        String method = request.getMethod();
        try {
            switch (method) {
                // Product operations
                case "getProducts":       return handleGetProducts(request);
                case "addProduct":        return handleAddProduct(request, sourceClient);
                case "productExists":     return handleProductExists(request);

                // Inventory operations
                case "getInventory":      return handleGetInventory(request);
                case "getInventoryAll":   return handleGetInventoryAll(request);

                // Sale operations
                case "processSale":       return handleProcessSale(request, sourceClient);

                // Stock operations
                case "addStockBatch":     return handleAddStockBatch(request, sourceClient);
                case "getStockBatches":   return handleGetStockBatches(request);
                case "getStockBatchesAll": return handleGetStockBatchesAll(request);
                case "transferStock":     return handleTransferStock(request, sourceClient);

                // User operations
                case "authenticateUser":  return handleAuthenticateUser(request);
                case "registerUser":      return handleRegisterUser(request);

                // Report operations
                case "generateReport":    return handleGenerateReport(request);

                // Server info
                case "ping":              return Response.success(request.getId(),
                                                  JsonProtocol.toJsonElement("pong"));
                case "getServerStats":    return handleGetServerStats(request);

                default:
                    return Response.error(request.getId(), "Unknown method: " + method);
            }
        } catch (Exception e) {
            System.err.println("[DISPATCHER] Error processing " + method + ": " + e.getMessage());
            return Response.error(request.getId(), e.getMessage());
        }
    }

    // ==================== Product Handlers ====================

    private Response handleGetProducts(Request request) {
        List<Product> products = productRepository.findAll();
        JsonArray arr = new JsonArray();
        for (Product p : products) {
            arr.add(productToJson(p));
        }
        return Response.success(request.getId(), arr);
    }

    private Response handleAddProduct(Request request, ClientHandler source) {
        String code = request.getStringParam("code");
        String name = request.getStringParam("name");
        String unit = request.getStringParam("unit");
        if (unit == null || unit.isEmpty()) unit = "pcs";
        double price = request.getDoubleParam("price");
        double discount = request.getDoubleParam("discountPercentage");

        if (productRepository.exists(code)) {
            return Response.error(request.getId(), "Product code already exists: " + code);
        }

        Product product = new Product.Builder()
                .code(code).name(name).unit(unit)
                .price(price).discountPercentage(discount)
                .build();
        productRepository.save(product);
        inventoryRepository.save(new Inventory(code));

        // Handle initial stock if provided
        int initialStock = request.getIntParam("initialStock");
        String expiryStr = request.getStringParam("expiryDate");
        if (initialStock > 0) {
            LocalDate expiry = (expiryStr != null && !expiryStr.isEmpty())
                    ? LocalDate.parse(expiryStr)
                    : LocalDate.now().plusYears(1);
            try {
                addStockBatchUseCase.execute(code, initialStock, expiry);
            } catch (Exception e) {
                System.err.println("[DISPATCHER] Warning: Failed to add initial stock: " + e.getMessage());
            }
        }

        clientRegistry.broadcastNotification("PRODUCT_CHANGED", null);
        return Response.success(request.getId(), productToJson(product));
    }

    private Response handleProductExists(Request request) {
        String code = request.getStringParam("code");
        boolean exists = productRepository.exists(code);
        return Response.success(request.getId(), JsonProtocol.toJsonElement(exists));
    }

    // ==================== Inventory Handlers ====================

    private Response handleGetInventory(Request request) {
        String code = request.getStringParam("productCode");
        Optional<Inventory> inv = inventoryRepository.findByProductCode(code);
        if (inv.isPresent()) {
            return Response.success(request.getId(), inventoryToJson(inv.get()));
        }
        return Response.error(request.getId(), "Inventory not found for: " + code);
    }

    private Response handleGetInventoryAll(Request request) {
        List<Inventory> inventories = inventoryRepository.findAll();
        JsonArray arr = new JsonArray();
        for (Inventory inv : inventories) {
            JsonObject obj = inventoryToJson(inv);
            // Add product name
            productRepository.findByCode(inv.getProductCode())
                    .ifPresent(p -> obj.addProperty("productName", p.getName()));
            arr.add(obj);
        }
        return Response.success(request.getId(), arr);
    }

    // ==================== Sale Handlers ====================

    @SuppressWarnings("unchecked")
    private Response handleProcessSale(Request request, ClientHandler source) {
        // Lock to prevent concurrent sales from causing race conditions
        inventoryLock.lock();
        try {
            List<?> rawItems = (List<?>) request.getParam("items");
            double cashTendered = request.getDoubleParam("cashTendered");
            String typeStr = request.getStringParam("transactionType");
            String customerId = request.getStringParam("customerId");

            Bill.TransactionType transactionType = typeStr != null
                    ? Bill.TransactionType.valueOf(typeStr)
                    : Bill.TransactionType.COUNTER;

            List<ProcessSaleUseCase.SaleRequest.SaleItem> saleItems = new ArrayList<>();
            if (rawItems != null) {
                for (Object rawItem : rawItems) {
                    if (rawItem instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) rawItem;
                        String productCode = String.valueOf(itemMap.get("productCode"));
                        int quantity = ((Number) itemMap.get("quantity")).intValue();
                        saleItems.add(new ProcessSaleUseCase.SaleRequest.SaleItem(productCode, quantity));
                    }
                }
            }

            ProcessSaleUseCase.SaleRequest saleRequest = new ProcessSaleUseCase.SaleRequest(
                    saleItems, cashTendered, transactionType, customerId);

            Bill bill = processSaleUseCase.execute(saleRequest);

            // Notify all clients about inventory change
            clientRegistry.broadcastNotification("INVENTORY_CHANGED", null);
            clientRegistry.broadcastNotification("SALE_COMPLETED", null);

            return Response.success(request.getId(), billToJson(bill));

        } catch (ProcessSaleUseCase.SaleException e) {
            return Response.error(request.getId(), "Sale failed: " + e.getMessage());
        } finally {
            inventoryLock.unlock();
        }
    }

    // ==================== Stock Handlers ====================

    private Response handleAddStockBatch(Request request, ClientHandler source) {
        inventoryLock.lock();
        try {
            String code = request.getStringParam("productCode");
            int quantity = request.getIntParam("quantity");
            String expiryStr = request.getStringParam("expiryDate");
            LocalDate expiryDate = LocalDate.parse(expiryStr);

            StockBatch batch = addStockBatchUseCase.execute(code, quantity, expiryDate);

            clientRegistry.broadcastNotification("INVENTORY_CHANGED", null);

            JsonObject result = new JsonObject();
            result.addProperty("batchId", batch.getBatchId());
            result.addProperty("success", true);
            return Response.success(request.getId(), result);

        } catch (Exception e) {
            return Response.error(request.getId(), "Add stock failed: " + e.getMessage());
        } finally {
            inventoryLock.unlock();
        }
    }

    private Response handleGetStockBatches(Request request) {
        String code = request.getStringParam("productCode");
        List<StockBatch> batches = stockBatchRepository.findByProductCode(code);
        JsonArray arr = new JsonArray();
        for (StockBatch b : batches) {
            arr.add(stockBatchToJson(b));
        }
        return Response.success(request.getId(), arr);
    }

    private Response handleGetStockBatchesAll(Request request) {
        List<StockBatch> batches = stockBatchRepository.findAll();
        JsonArray arr = new JsonArray();
        for (StockBatch b : batches) {
            arr.add(stockBatchToJson(b));
        }
        return Response.success(request.getId(), arr);
    }

    private Response handleTransferStock(Request request, ClientHandler source) {
        inventoryLock.lock();
        try {
            String code = request.getStringParam("productCode");
            int quantity = request.getIntParam("quantity");
            String typeStr = request.getStringParam("transferType");
            TransferStockUseCase.TransferType type = TransferStockUseCase.TransferType.valueOf(typeStr);

            transferStockUseCase.execute(code, quantity, type);

            clientRegistry.broadcastNotification("INVENTORY_CHANGED", null);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            return Response.success(request.getId(), result);

        } catch (Exception e) {
            return Response.error(request.getId(), "Transfer failed: " + e.getMessage());
        } finally {
            inventoryLock.unlock();
        }
    }

    // ==================== User Handlers ====================

    private Response handleAuthenticateUser(Request request) {
        String email = request.getStringParam("email");
        String password = request.getStringParam("password");
        try {
            User user = authenticateUserUseCase.execute(email, password);
            JsonObject obj = new JsonObject();
            obj.addProperty("userId", user.getUserId());
            obj.addProperty("name", user.getName());
            obj.addProperty("email", user.getEmail());
            obj.addProperty("address", user.getAddress());
            return Response.success(request.getId(), obj);
        } catch (AuthenticateUserUseCase.AuthenticationException e) {
            return Response.error(request.getId(), e.getMessage());
        }
    }

    private Response handleRegisterUser(Request request) {
        String name = request.getStringParam("name");
        String email = request.getStringParam("email");
        String password = request.getStringParam("password");
        String address = request.getStringParam("address");
        try {
            registerUserUseCase.execute(name, email, password, address);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            return Response.success(request.getId(), result);
        } catch (RegisterUserUseCase.RegistrationException e) {
            return Response.error(request.getId(), e.getMessage());
        }
    }

    // ==================== Report Handlers ====================

    private synchronized Response handleGenerateReport(Request request) {
        String reportType = request.getStringParam("reportType");
        
        String dateStr = request.getStringParam("date");
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : null;
        
        String typeStr = request.getStringParam("transactionType");
        Bill.TransactionType txType = (typeStr != null && !typeStr.isEmpty() && !typeStr.equals("ALL"))
                ? Bill.TransactionType.valueOf(typeStr) : null;
                
        String reportContent;

        switch (reportType) {
            case "DAILY_SALES":
                LocalDate salesDate = date != null ? date : LocalDate.now();
                reportContent = new DailySalesReport(billRepository, salesDate, txType).generateReport();
                break;
            case "RESHELVE":
                reportContent = new ReshelveReport(inventoryRepository, productRepository, date).generateReport();
                break;
            case "REORDER":
                reportContent = new ReorderLevelsReport(inventoryRepository, productRepository, date).generateReport();
                break;
            case "STOCK":
                reportContent = new StockReport(stockBatchRepository, productRepository, date).generateReport();
                break;
            case "BILL":
                reportContent = new BillReport(billRepository, txType, date).generateReport();
                break;
            default:
                return Response.error(request.getId(), "Unknown report type: " + reportType);
        }

        return Response.success(request.getId(), JsonProtocol.toJsonElement(reportContent));
    }

    // ==================== Server Info ====================

    private Response handleGetServerStats(Request request) {
        JsonObject stats = new JsonObject();
        stats.addProperty("connectedClients", clientRegistry.getClientCount());
        return Response.success(request.getId(), stats);
    }

    // ==================== JSON Conversion Helpers ====================

    private JsonObject productToJson(Product p) {
        JsonObject obj = new JsonObject();
        obj.addProperty("code", p.getCode());
        obj.addProperty("name", p.getName());
        obj.addProperty("unit", p.getUnit());
        obj.addProperty("price", p.getPrice());
        obj.addProperty("discountPercentage", p.getDiscountPercentage());
        obj.addProperty("discountedPrice", p.getDiscountedPrice());
        return obj;
    }

    private JsonObject inventoryToJson(Inventory inv) {
        JsonObject obj = new JsonObject();
        obj.addProperty("productCode", inv.getProductCode());
        obj.addProperty("shelfQuantity", inv.getShelfQuantity());
        obj.addProperty("storeQuantity", inv.getStoreQuantity());
        obj.addProperty("onlineQuantity", inv.getOnlineQuantity());
        obj.addProperty("totalQuantity", inv.getTotalQuantity());
        obj.addProperty("belowReorderLevel", inv.isBelowReorderLevel());
        return obj;
    }

    private JsonObject stockBatchToJson(StockBatch b) {
        JsonObject obj = new JsonObject();
        obj.addProperty("batchId", b.getBatchId());
        obj.addProperty("productCode", b.getProductCode());
        obj.addProperty("purchaseDate", b.getPurchaseDate().toString());
        obj.addProperty("quantity", b.getQuantity());
        obj.addProperty("expiryDate", b.getExpiryDate().toString());
        obj.addProperty("expired", b.isExpired());
        obj.addProperty("daysUntilExpiry", b.getDaysUntilExpiry());
        String status = b.isExpired() ? "EXPIRED" : (b.getDaysUntilExpiry() < 30 ? "EXPIRING" : "OK");
        obj.addProperty("status", status);
        return obj;
    }

    private JsonObject billToJson(Bill bill) {
        JsonObject obj = new JsonObject();
        obj.addProperty("serialNumber", bill.getSerialNumber());
        obj.addProperty("billDate", bill.getBillDate().toString());
        obj.addProperty("subtotal", bill.getSubtotal());
        obj.addProperty("discount", bill.getDiscount());
        obj.addProperty("total", bill.getTotal());
        obj.addProperty("cashTendered", bill.getCashTendered());
        obj.addProperty("change", bill.getChange());
        obj.addProperty("transactionType", bill.getTransactionType().name());
        obj.addProperty("customerId", bill.getCustomerId());

        JsonArray items = new JsonArray();
        for (Bill.BillItem item : bill.getItems()) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("productCode", item.getProductCode());
            itemObj.addProperty("productName", item.getProductName());
            itemObj.addProperty("unit", item.getUnit());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("price", item.getPrice());
            itemObj.addProperty("discountPercentage", item.getDiscountPercentage());
            itemObj.addProperty("itemTotal", item.getItemTotal());
            itemObj.addProperty("discountAmount", item.getDiscountAmount());
            itemObj.addProperty("finalPrice", item.getFinalPrice());
            items.add(itemObj);
        }
        obj.add("items", items);
        return obj;
    }
}
