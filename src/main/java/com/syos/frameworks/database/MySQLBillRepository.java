package com.syos.frameworks.database;

import com.syos.entities.Bill;
import com.syos.usecases.repositories.BillRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL implementation of BillRepository.
 */
public class MySQLBillRepository implements BillRepository {
    private final DatabaseManager dbManager;

    public MySQLBillRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void save(Bill bill) {
        String billSql = "INSERT INTO bills (bill_date, subtotal, discount, total, cash_tendered, change_amount, transaction_type, customer_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO bill_items (bill_serial_number, product_code, product_name, unit, quantity, price, discount_percentage) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement billStmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                billStmt.setTimestamp(1, Timestamp.valueOf(bill.getBillDate()));
                billStmt.setDouble(2, bill.getSubtotal());
                billStmt.setDouble(3, bill.getDiscount());
                billStmt.setDouble(4, bill.getTotal());
                billStmt.setDouble(5, bill.getCashTendered());
                billStmt.setDouble(6, bill.getChange());
                billStmt.setString(7, bill.getTransactionType().name());
                billStmt.setString(8, bill.getCustomerId());

                billStmt.executeUpdate();

                ResultSet rs = billStmt.getGeneratedKeys();
                if (rs.next()) {
                    int billSerialNumber = rs.getInt(1);

                    try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                        for (Bill.BillItem item : bill.getItems()) {
                            itemStmt.setInt(1, billSerialNumber);
                            itemStmt.setString(2, item.getProductCode());
                            itemStmt.setString(3, item.getProductName());
                            itemStmt.setString(4, item.getUnit());
                            itemStmt.setInt(5, item.getQuantity());
                            itemStmt.setDouble(6, item.getPrice());
                            itemStmt.setDouble(7, item.getDiscountPercentage());
                            itemStmt.addBatch();
                        }
                        itemStmt.executeBatch();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error saving bill: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Bill> findBySerialNumber(int serialNumber) {
        String sql = "SELECT * FROM bills WHERE serial_number = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, serialNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToBill(rs, conn));
            }
            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Error finding bill: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bill> findAll() {
        String sql = "SELECT * FROM bills ORDER BY serial_number DESC";
        List<Bill> bills = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs, conn));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all bills: " + e.getMessage(), e);
        }

        return bills;
    }

    @Override
    public List<Bill> findByDate(LocalDate date) {
        String sql = "SELECT * FROM bills WHERE DATE(bill_date) = ? ORDER BY serial_number DESC";
        List<Bill> bills = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs, conn));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding bills by date: " + e.getMessage(), e);
        }

        return bills;
    }

    @Override
    public List<Bill> findByTransactionType(Bill.TransactionType type) {
        String sql = "SELECT * FROM bills WHERE transaction_type = ? ORDER BY serial_number DESC";
        List<Bill> bills = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs, conn));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding bills by type: " + e.getMessage(), e);
        }

        return bills;
    }

    @Override
    public List<Bill> findByDateAndType(LocalDate date, Bill.TransactionType type) {
        String sql = "SELECT * FROM bills WHERE DATE(bill_date) = ? AND transaction_type = ? ORDER BY serial_number DESC";
        List<Bill> bills = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setString(2, type.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                bills.add(mapResultSetToBill(rs, conn));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding bills by date and type: " + e.getMessage(), e);
        }

        return bills;
    }

    @Override
    public int getNextSerialNumber() {
        String sql = "SELECT COALESCE(MAX(serial_number), 0) + 1 FROM bills";

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 1;

        } catch (SQLException e) {
            throw new RuntimeException("Error getting next serial number: " + e.getMessage(), e);
        }
    }

    private Bill mapResultSetToBill(ResultSet rs, Connection conn) throws SQLException {
        int serialNumber = rs.getInt("serial_number");
        LocalDateTime billDate = rs.getTimestamp("bill_date").toLocalDateTime();
        double cashTendered = rs.getDouble("cash_tendered");
        Bill.TransactionType transactionType = Bill.TransactionType.valueOf(rs.getString("transaction_type"));
        String customerId = rs.getString("customer_id");

        Bill.Builder builder = new Bill.Builder()
                .serialNumber(serialNumber)
                .billDate(billDate)
                .cashTendered(cashTendered)
                .transactionType(transactionType)
                .customerId(customerId);

        List<Bill.BillItem> items = getBillItems(serialNumber, conn);
        for (Bill.BillItem item : items) {
            builder.addItem(item);
        }

        return builder.build();
    }

    private List<Bill.BillItem> getBillItems(int billSerialNumber, Connection conn) throws SQLException {
        String sql = "SELECT * FROM bill_items WHERE bill_serial_number = ?";
        List<Bill.BillItem> items = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, billSerialNumber);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                items.add(new Bill.BillItem(
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getString("unit"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getDouble("discount_percentage")));
            }
        }

        return items;
    }
}
