package com.syos.usecases;

import com.syos.entities.Bill;
import com.syos.entities.Inventory;
import com.syos.entities.Product;
import com.syos.entities.StockBatch;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.repositories.BillRepository;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.ProductRepository;
import com.syos.usecases.repositories.StockBatchRepository;
import com.syos.usecases.strategies.StockSelectionStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for processing sales transactions.
 * Follows Single Responsibility Principle - handles only sale processing.
 * Implements Command Pattern through execute method.
 */
public class ProcessSaleUseCase {
    private final ProductRepository productRepository;
    private final BillRepository billRepository;
    private final InventoryRepository inventoryRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockSelectionStrategy stockSelectionStrategy;
    private final InventorySubject inventorySubject;

    public ProcessSaleUseCase(ProductRepository productRepository,
            BillRepository billRepository,
            InventoryRepository inventoryRepository,
            StockBatchRepository stockBatchRepository,
            StockSelectionStrategy stockSelectionStrategy,
            InventorySubject inventorySubject) {
        this.productRepository = productRepository;
        this.billRepository = billRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.stockSelectionStrategy = stockSelectionStrategy;
        this.inventorySubject = inventorySubject;
    }

    /**
     * Processes a sale transaction.
     * 
     * @param request Sale request containing items and payment details
     * @return Generated bill
     * @throws SaleException if sale cannot be processed
     */
    public Bill execute(SaleRequest request) throws SaleException {
        validateRequest(request);

        // Build bill items
        List<Bill.BillItem> billItems = new ArrayList<>();
        Map<String, Integer> itemQuantities = new HashMap<>();

        for (SaleRequest.SaleItem saleItem : request.getItems()) {
            Product product = productRepository.findByCode(saleItem.getProductCode())
                    .orElseThrow(() -> new SaleException("Product not found: " + saleItem.getProductCode()));

            // Check inventory availability
            Inventory inventory = inventoryRepository.findByProductCode(product.getCode())
                    .orElseThrow(() -> new SaleException("Inventory not found for product: " + product.getCode()));

            int availableQuantity = request.getTransactionType() == Bill.TransactionType.COUNTER
                    ? inventory.getShelfQuantity()
                    : inventory.getOnlineQuantity();

            if (availableQuantity < saleItem.getQuantity()) {
                throw new SaleException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + availableQuantity +
                        ", Requested: " + saleItem.getQuantity());
            }

            // Create bill item
            Bill.BillItem billItem = new Bill.BillItem(
                    product.getCode(),
                    product.getName(),
                    product.getUnit(),
                    saleItem.getQuantity(),
                    product.getPrice(),
                    product.getDiscountPercentage());
            billItems.add(billItem);
            itemQuantities.put(product.getCode(), saleItem.getQuantity());
        }

        // Create bill
        Bill bill = new Bill.Builder()
                .serialNumber(billRepository.getNextSerialNumber())
                .items(billItems)
                .cashTendered(request.getCashTendered())
                .transactionType(request.getTransactionType())
                .customerId(request.getCustomerId())
                .build();

        // Save bill
        billRepository.save(bill);

        // Update inventory
        for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            String productCode = entry.getKey();
            int quantity = entry.getValue();

            Inventory inventory = inventoryRepository.findByProductCode(productCode).get();

            if (request.getTransactionType() == Bill.TransactionType.COUNTER) {
                inventory.reduceFromShelf(quantity);
                reduceStockBatches(productCode, quantity);
            } else {
                inventory.reduceFromOnline(quantity);
            }

            inventoryRepository.update(inventory);
            inventorySubject.notifyInventoryChanged(inventory);
        }

        return bill;
    }

    private void reduceStockBatches(String productCode, int quantityNeeded) throws SaleException {
        List<StockBatch> batches = stockBatchRepository.findByProductCode(productCode);
        int remainingQuantity = quantityNeeded;

        while (remainingQuantity > 0) {
            StockBatch selectedBatch = stockSelectionStrategy.selectBatch(batches);

            if (selectedBatch == null) {
                throw new SaleException("No suitable stock batch available for product: " + productCode);
            }

            int quantityToReduce = Math.min(remainingQuantity, selectedBatch.getQuantity());
            selectedBatch.reduceQuantity(quantityToReduce);
            stockBatchRepository.update(selectedBatch);

            remainingQuantity -= quantityToReduce;
        }
    }

    private void validateRequest(SaleRequest request) throws SaleException {
        if (request == null) {
            throw new SaleException("Sale request cannot be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new SaleException("Sale must have at least one item");
        }
        if (request.getCashTendered() < 0) {
            throw new SaleException("Cash tendered cannot be negative");
        }
    }

    /**
     * Request object for sale processing.
     */
    public static class SaleRequest {
        private final List<SaleItem> items;
        private final double cashTendered;
        private final Bill.TransactionType transactionType;
        private final String customerId;

        public SaleRequest(List<SaleItem> items, double cashTendered,
                Bill.TransactionType transactionType, String customerId) {
            this.items = items;
            this.cashTendered = cashTendered;
            this.transactionType = transactionType;
            this.customerId = customerId;
        }

        public List<SaleItem> getItems() {
            return items;
        }

        public double getCashTendered() {
            return cashTendered;
        }

        public Bill.TransactionType getTransactionType() {
            return transactionType;
        }

        public String getCustomerId() {
            return customerId;
        }

        public static class SaleItem {
            private final String productCode;
            private final int quantity;

            public SaleItem(String productCode, int quantity) {
                this.productCode = productCode;
                this.quantity = quantity;
            }

            public String getProductCode() {
                return productCode;
            }

            public int getQuantity() {
                return quantity;
            }
        }
    }

    public static class SaleException extends Exception {
        public SaleException(String message) {
            super(message);
        }
    }
}
