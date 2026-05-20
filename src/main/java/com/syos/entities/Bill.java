package com.syos.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a bill/invoice in the system.
 * Immutable after creation - follows Builder pattern.
 */
public class Bill {
    private final int serialNumber;
    private final LocalDateTime billDate;
    private final List<BillItem> items;
    private final double subtotal;
    private final double discount;
    private final double total;
    private final double cashTendered;
    private final double change;
    private final TransactionType transactionType;
    private final String customerId; // For online transactions

    private Bill(Builder builder) {
        this.serialNumber = builder.serialNumber;
        this.billDate = builder.billDate;
        this.items = new ArrayList<>(builder.items);
        this.subtotal = builder.subtotal;
        this.discount = builder.discount;
        this.total = builder.total;
        this.cashTendered = builder.cashTendered;
        this.change = builder.change;
        this.transactionType = builder.transactionType;
        this.customerId = builder.customerId;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public List<BillItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
    }

    public double getCashTendered() {
        return cashTendered;
    }

    public double getChange() {
        return change;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "serialNumber=" + serialNumber +
                ", billDate=" + billDate +
                ", total=" + total +
                ", transactionType=" + transactionType +
                '}';
    }

    /**
     * Builder pattern for Bill construction.
     */
    public static class Builder {
        private int serialNumber;
        private LocalDateTime billDate = LocalDateTime.now();
        private List<BillItem> items = new ArrayList<>();
        private double subtotal;
        private double discount;
        private double total;
        private double cashTendered;
        private double change;
        private TransactionType transactionType = TransactionType.COUNTER;
        private String customerId;

        public Builder serialNumber(int serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder billDate(LocalDateTime billDate) {
            this.billDate = billDate;
            return this;
        }

        public Builder addItem(BillItem item) {
            this.items.add(item);
            return this;
        }

        public Builder items(List<BillItem> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        public Builder subtotal(double subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder discount(double discount) {
            this.discount = discount;
            return this;
        }

        public Builder total(double total) {
            this.total = total;
            return this;
        }

        public Builder cashTendered(double cashTendered) {
            this.cashTendered = cashTendered;
            return this;
        }

        public Builder change(double change) {
            this.change = change;
            return this;
        }

        public Builder transactionType(TransactionType transactionType) {
            this.transactionType = transactionType;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Bill build() {
            calculateTotals();
            validate();
            return new Bill(this);
        }

        private void calculateTotals() {
            subtotal = items.stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();
            discount = items.stream()
                    .mapToDouble(BillItem::getDiscountAmount)
                    .sum();
            total = subtotal - discount;
            change = cashTendered - total;
        }

        private void validate() {
            if (items.isEmpty()) {
                throw new IllegalArgumentException("Bill must have at least one item");
            }
            if (cashTendered < total) {
                throw new IllegalArgumentException("Cash tendered must be greater than or equal to total");
            }
        }
    }

    /**
     * Represents an individual item in the bill.
     */
    public static class BillItem {
        private final String productCode;
        private final String productName;
        private final String unit;
        private final int quantity;
        private final double price;
        private final double discountPercentage;

        public BillItem(String productCode, String productName, String unit, int quantity, double price,
                double discountPercentage) {
            this.productCode = productCode;
            this.productName = productName;
            this.unit = unit;
            this.quantity = quantity;
            this.price = price;
            this.discountPercentage = discountPercentage;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getProductName() {
            return productName;
        }

        public String getUnit() {
            return unit;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }

        public double getDiscountPercentage() {
            return discountPercentage;
        }

        public double getItemTotal() {
            return price * quantity;
        }

        public double getDiscountAmount() {
            return getItemTotal() * (discountPercentage / 100.0);
        }

        public double getFinalPrice() {
            return getItemTotal() - getDiscountAmount();
        }

        @Override
        public String toString() {
            return "BillItem{" +
                    "productName='" + productName + '\'' +
                    ", quantity=" + quantity +
                    ", price=" + price +
                    ", discountPercentage=" + discountPercentage +
                    '}';
        }
    }

    public enum TransactionType {
        COUNTER,
        ONLINE
    }
}
