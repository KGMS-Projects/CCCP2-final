package com.syos.frameworks.database;

import com.syos.entities.StockBatch;
import com.syos.usecases.repositories.StockBatchRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL implementation of StockBatchRepository.
 */
public class MySQLStockBatchRepository implements StockBatchRepository {
    private final DatabaseManager dbManager;

    public MySQLStockBatchRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void save(StockBatch stockBatch) {
        String sql = "INSERT INTO stock_batches (batch_id, product_code, purchase_date, quantity, expiry_date) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stockBatch.getBatchId());
            stmt.setString(2, stockBatch.getProductCode());
            stmt.setDate(3, Date.valueOf(stockBatch.getPurchaseDate()));
            stmt.setInt(4, stockBatch.getQuantity());
            stmt.setDate(5, Date.valueOf(stockBatch.getExpiryDate()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving stock batch: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<StockBatch> findById(String batchId) {
        String sql = "SELECT * FROM stock_batches WHERE batch_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, batchId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToStockBatch(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error finding stock batch: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StockBatch> findByProductCode(String productCode) {
        String sql = "SELECT * FROM stock_batches WHERE product_code = ? ORDER BY purchase_date";
        List<StockBatch> batches = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, productCode);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                batches.add(mapResultSetToStockBatch(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding stock batches: " + e.getMessage(), e);
        }

        return batches;
    }

    @Override
    public List<StockBatch> findAll() {
        String sql = "SELECT * FROM stock_batches ORDER BY product_code, purchase_date";
        List<StockBatch> batches = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                batches.add(mapResultSetToStockBatch(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all stock batches: " + e.getMessage(), e);
        }

        return batches;
    }

    @Override
    public void update(StockBatch stockBatch) {
        String sql = "UPDATE stock_batches SET quantity = ? WHERE batch_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stockBatch.getQuantity());
            stmt.setString(2, stockBatch.getBatchId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Stock batch not found: " + stockBatch.getBatchId());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error updating stock batch: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String batchId) {
        String sql = "DELETE FROM stock_batches WHERE batch_id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, batchId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting stock batch: " + e.getMessage(), e);
        }
    }

    private StockBatch mapResultSetToStockBatch(ResultSet rs) throws SQLException {
        String batchId = rs.getString("batch_id");
        String productCode = rs.getString("product_code");
        LocalDate purchaseDate = rs.getDate("purchase_date").toLocalDate();
        int quantity = rs.getInt("quantity");
        LocalDate expiryDate = rs.getDate("expiry_date").toLocalDate();

        // Use constructor with batch_id to preserve the database ID
        return new StockBatch(batchId, productCode, purchaseDate, quantity, expiryDate);
    }
}
