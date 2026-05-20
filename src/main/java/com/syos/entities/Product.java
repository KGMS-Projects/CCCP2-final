package com.syos.entities;

import java.util.Objects;

/**
 * Core entity representing a product in the system.
 * Follows Single Responsibility Principle - only handles product data.
 */
public class Product {
    private final String code;
    private final String name;
    private final String unit; // Unit of measurement (kg, L, pcs, etc.)
    private final double price;
    private final double discountPercentage;

    // Private constructor - use Builder pattern
    private Product(Builder builder) {
        this.code = builder.code;
        this.name = builder.name;
        this.unit = builder.unit;
        this.price = builder.price;
        this.discountPercentage = builder.discountPercentage;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public double getPrice() {
        return price;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public double getDiscountedPrice() {
        return price * (1 - discountPercentage / 100.0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Product product = (Product) o;
        return Objects.equals(code, product.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Product{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", unit='" + unit + '\'' +
                ", price=" + price +
                ", discountPercentage=" + discountPercentage +
                '}';
    }

    /**
     * Builder pattern for Product construction.
     * Provides flexible and readable object creation.
     */
    public static class Builder {
        private String code;
        private String name;
        private String unit = "pcs"; // Default unit
        private double price;
        private double discountPercentage = 0.0;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public Builder price(double price) {
            this.price = price;
            return this;
        }

        public Builder discountPercentage(double discountPercentage) {
            this.discountPercentage = discountPercentage;
            return this;
        }

        public Product build() {
            validateProduct();
            return new Product(this);
        }

        private void validateProduct() {
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Product code cannot be empty");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Product name cannot be empty");
            }
            if (unit == null || unit.trim().isEmpty()) {
                throw new IllegalArgumentException("Product unit cannot be empty");
            }
            if (price < 0) {
                throw new IllegalArgumentException("Product price cannot be negative");
            }
            if (discountPercentage < 0 || discountPercentage > 100) {
                throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
            }
        }
    }
}
