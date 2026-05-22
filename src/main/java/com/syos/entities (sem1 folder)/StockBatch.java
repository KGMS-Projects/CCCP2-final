package com.syos.entities;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a batch of stock for a product.
 * Follows Single Responsibility Principle.
 */
public class StockBatch {
    private static int batchCounter = 0;
    private final String productCode;
    private final LocalDate purchaseDate;
    private int quantity;
    private final LocalDate expiryDate;
    private final String batchId;

    // Constructor for creating new batches (generates new batch ID)
    public StockBatch(String productCode, LocalDate purchaseDate, int quantity, LocalDate expiryDate) {
        validateStockBatch(productCode, quantity, purchaseDate, expiryDate);
        this.productCode = productCode;
        this.purchaseDate = purchaseDate;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.batchId = generateBatchId();
    }

    // Constructor for loading existing batches from database (uses existing batch
    // ID)
    public StockBatch(String batchId, String productCode, LocalDate purchaseDate, int quantity, LocalDate expiryDate) {
        validateStockBatch(productCode, quantity, purchaseDate, expiryDate);
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be empty");
        }
        this.batchId = batchId;
        this.productCode = productCode;
        this.purchaseDate = purchaseDate;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    private String generateBatchId() {
        return "B" + (++batchCounter);
    }

    private void validateStockBatch(String productCode, int quantity, LocalDate purchaseDate, LocalDate expiryDate) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (purchaseDate == null) {
            throw new IllegalArgumentException("Purchase date cannot be null");
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date cannot be null");
        }
        if (expiryDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before purchase date");
        }
    }

    public String getProductCode() {
        return productCode;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getBatchId() {
        return batchId;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public void reduceQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > quantity) {
            throw new IllegalArgumentException("Cannot reduce more than available quantity");
        }
        this.quantity -= amount;
    }

    public int getDaysUntilExpiry() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StockBatch that = (StockBatch) o;
        return Objects.equals(batchId, that.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId);
    }

    @Override
    public String toString() {
        return "StockBatch{" +
                "productCode='" + productCode + '\'' +
                ", purchaseDate=" + purchaseDate +
                ", quantity=" + quantity +
                ", expiryDate=" + expiryDate +
                ", batchId='" + batchId + '\'' +
                '}';
    }
}
