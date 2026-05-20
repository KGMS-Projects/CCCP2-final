package com.syos.entities;

/**
 * Represents inventory for a product across different storage locations.
 * Follows Single Responsibility Principle.
 */
public class Inventory {
    private final String productCode;
    private int shelfQuantity;
    private int storeQuantity;
    private int onlineQuantity;

    public Inventory(String productCode) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty");
        }
        this.productCode = productCode;
        this.shelfQuantity = 0;
        this.storeQuantity = 0;
        this.onlineQuantity = 0;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getShelfQuantity() {
        return shelfQuantity;
    }

    public int getStoreQuantity() {
        return storeQuantity;
    }

    public int getOnlineQuantity() {
        return onlineQuantity;
    }

    public int getTotalQuantity() {
        return shelfQuantity + storeQuantity + onlineQuantity;
    }

    public void addToShelf(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.shelfQuantity += quantity;
    }

    public void addToStore(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.storeQuantity += quantity;
    }

    public void addToOnline(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.onlineQuantity += quantity;
    }

    public void reduceFromShelf(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > shelfQuantity) {
            throw new IllegalArgumentException("Insufficient shelf quantity");
        }
        this.shelfQuantity -= quantity;
    }

    public void reduceFromStore(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > storeQuantity) {
            throw new IllegalArgumentException("Insufficient store quantity");
        }
        this.storeQuantity -= quantity;
    }

    public void reduceFromOnline(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > onlineQuantity) {
            throw new IllegalArgumentException("Insufficient online quantity");
        }
        this.onlineQuantity -= quantity;
    }

    public void transferFromStoreToShelf(int quantity) {
        reduceFromStore(quantity);
        addToShelf(quantity);
    }

    public void transferFromStoreToOnline(int quantity) {
        reduceFromStore(quantity);
        addToOnline(quantity);
    }

    public boolean isBelowReorderLevel() {
        return getTotalQuantity() < 50;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "productCode='" + productCode + '\'' +
                ", shelfQuantity=" + shelfQuantity +
                ", storeQuantity=" + storeQuantity +
                ", onlineQuantity=" + onlineQuantity +
                ", totalQuantity=" + getTotalQuantity() +
                '}';
    }
}
