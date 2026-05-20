package com.syos.frameworks.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection manager using Singleton pattern.
 * Uses HikariCP for connection pooling (best practice for production).
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/syos_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; // Change this to your MySQL password

    private DatabaseManager() {
        initializeDataSource();
        createTablesIfNotExist();
    }

    /**
     * Gets the singleton instance (thread-safe).
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initializes HikariCP connection pool.
     */
    private void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);

        System.out.println("✓ Database connection pool initialized");
    }

    /**
     * Gets a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Creates database tables if they don't exist.
     */
    private void createTablesIfNotExist() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            // Create products table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "code VARCHAR(50) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "unit VARCHAR(20) NOT NULL DEFAULT 'pcs'," +
                    "price DECIMAL(10, 2) NOT NULL," +
                    "discount_percentage DECIMAL(5, 2) DEFAULT 0.0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Create inventory table
            stmt.execute("CREATE TABLE IF NOT EXISTS inventory (" +
                    "product_code VARCHAR(50) PRIMARY KEY," +
                    "shelf_quantity INT DEFAULT 0," +
                    "store_quantity INT DEFAULT 0," +
                    "online_quantity INT DEFAULT 0," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_code) REFERENCES products(code) ON DELETE CASCADE" +
                    ")");

            // Create stock_batches table
            stmt.execute("CREATE TABLE IF NOT EXISTS stock_batches (" +
                    "batch_id VARCHAR(255) PRIMARY KEY," +
                    "product_code VARCHAR(50) NOT NULL," +
                    "purchase_date DATE NOT NULL," +
                    "quantity INT NOT NULL," +
                    "expiry_date DATE NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (product_code) REFERENCES products(code) ON DELETE CASCADE" +
                    ")");

            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id VARCHAR(255) PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "email VARCHAR(255) UNIQUE NOT NULL," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "address TEXT," +
                    "registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // Create bills table
            stmt.execute("CREATE TABLE IF NOT EXISTS bills (" +
                    "serial_number INT PRIMARY KEY AUTO_INCREMENT," +
                    "bill_date TIMESTAMP NOT NULL," +
                    "subtotal DECIMAL(10, 2) NOT NULL," +
                    "discount DECIMAL(10, 2) NOT NULL," +
                    "total DECIMAL(10, 2) NOT NULL," +
                    "cash_tendered DECIMAL(10, 2) NOT NULL," +
                    "change_amount DECIMAL(10, 2) NOT NULL," +
                    "transaction_type VARCHAR(20) NOT NULL," +
                    "customer_id VARCHAR(255)," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE SET NULL" +
                    ")");

            // Create bill_items table
            stmt.execute("CREATE TABLE IF NOT EXISTS bill_items (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "bill_serial_number INT NOT NULL," +
                    "product_code VARCHAR(50) NOT NULL," +
                    "product_name VARCHAR(255) NOT NULL," +
                    "unit VARCHAR(20) NOT NULL DEFAULT 'pcs'," +
                    "quantity INT NOT NULL," +
                    "price DECIMAL(10, 2) NOT NULL," +
                    "discount_percentage DECIMAL(5, 2) NOT NULL," +
                    "FOREIGN KEY (bill_serial_number) REFERENCES bills(serial_number) ON DELETE CASCADE," +
                    "FOREIGN KEY (product_code) REFERENCES products(code) ON DELETE RESTRICT" +
                    ")");

            // Add unit column to existing tables if not present (for backward
            // compatibility)
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN unit VARCHAR(20) NOT NULL DEFAULT 'pcs'");
                System.out.println("✓ Added 'unit' column to products table");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            try {
                stmt.execute("ALTER TABLE bill_items ADD COLUMN unit VARCHAR(20) NOT NULL DEFAULT 'pcs'");
                System.out.println("✓ Added 'unit' column to bill_items table");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            System.out.println("✓ Database tables created/verified successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error creating database tables: " + e.getMessage());
            System.err.println("Please ensure MySQL is running and the database 'syos_db' exists.");
            System.err.println("You can create it with: CREATE DATABASE syos_db;");
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Closes the connection pool.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✓ Database connection pool closed");
        }
    }

    /**
     * Test database connection.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}
