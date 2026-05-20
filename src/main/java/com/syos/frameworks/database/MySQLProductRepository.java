package com.syos.frameworks.database;

import com.syos.entities.Product;
import com.syos.usecases.repositories.ProductRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL implementation of ProductRepository.
 * Implements Repository Pattern with JDBC.
 */
public class MySQLProductRepository implements ProductRepository {
    private final DatabaseManager dbManager;

    public MySQLProductRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void save(Product product) {
        String sql = "INSERT INTO products (code, name, unit, price, discount_percentage) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getCode());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getUnit());
            stmt.setDouble(4, product.getPrice());
            stmt.setDouble(5, product.getDiscountPercentage());

            stmt.executeUpdate();

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                throw new IllegalArgumentException("Product with code " + product.getCode() + " already exists");
            }
            throw new RuntimeException("Error saving product: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Product> findByCode(String code) {
        String sql = "SELECT * FROM products WHERE code = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToProduct(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error finding product: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY code";
        List<Product> products = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all products: " + e.getMessage(), e);
        }

        return products;
    }

    @Override
    public void update(Product product) {
        String sql = "UPDATE products SET name = ?, unit = ?, price = ?, discount_percentage = ? WHERE code = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setString(2, product.getUnit());
            stmt.setDouble(3, product.getPrice());
            stmt.setDouble(4, product.getDiscountPercentage());
            stmt.setString(5, product.getCode());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Product not found: " + product.getCode());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error updating product: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String code) {
        String sql = "DELETE FROM products WHERE code = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String code) {
        String sql = "SELECT COUNT(*) FROM products WHERE code = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Error checking product existence: " + e.getMessage(), e);
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product.Builder()
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .unit(rs.getString("unit"))
                .price(rs.getDouble("price"))
                .discountPercentage(rs.getDouble("discount_percentage"))
                .build();
    }
}
