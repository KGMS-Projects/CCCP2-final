package com.syos.frameworks.database;

import com.syos.entities.Inventory;
import com.syos.usecases.repositories.InventoryRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL implementation of InventoryRepository.
 */
public class MySQLInventoryRepository implements InventoryRepository {
    private final DatabaseManager dbManager;

    public MySQLInventoryRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void save(Inventory inventory) {
        String sql = "INSERT INTO inventory (product_code, shelf_quantity, store_quantity, online_quantity) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inventory.getProductCode());
            stmt.setInt(2, inventory.getShelfQuantity());
            stmt.setInt(3, inventory.getStoreQuantity());
            stmt.setInt(4, inventory.getOnlineQuantity());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error saving inventory: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Inventory> findByProductCode(String productCode) {
        String sql = "SELECT * FROM inventory WHERE product_code = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, productCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToInventory(rs));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error finding inventory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Inventory> findAll() {
        String sql = "SELECT * FROM inventory ORDER BY product_code";
        List<Inventory> inventories = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                inventories.add(mapResultSetToInventory(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all inventories: " + e.getMessage(), e);
        }

        return inventories;
    }

    @Override
    public void update(Inventory inventory) {
        String sql = "UPDATE inventory SET shelf_quantity = ?, store_quantity = ?, online_quantity = ? WHERE product_code = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inventory.getShelfQuantity());
            stmt.setInt(2, inventory.getStoreQuantity());
            stmt.setInt(3, inventory.getOnlineQuantity());
            stmt.setString(4, inventory.getProductCode());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalArgumentException("Inventory not found: " + inventory.getProductCode());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error updating inventory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Inventory> findBelowReorderLevel() {
        String sql = "SELECT * FROM inventory WHERE (shelf_quantity + store_quantity + online_quantity) < 50 ORDER BY product_code";
        List<Inventory> inventories = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                inventories.add(mapResultSetToInventory(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding low stock inventories: " + e.getMessage(), e);
        }

        return inventories;
    }

    private Inventory mapResultSetToInventory(ResultSet rs) throws SQLException {
        Inventory inventory = new Inventory(rs.getString("product_code"));

        int shelfQty = rs.getInt("shelf_quantity");
        int storeQty = rs.getInt("store_quantity");
        int onlineQty = rs.getInt("online_quantity");

        if (shelfQty > 0)
            inventory.addToShelf(shelfQty);
        if (storeQty > 0)
            inventory.addToStore(storeQty);
        if (onlineQty > 0)
            inventory.addToOnline(onlineQty);

        return inventory;
    }
}
