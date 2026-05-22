package com.syos.usecases.reports;

import com.syos.entities.Inventory;
import com.syos.entities.Product;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.ProductRepository;

import java.util.List;
import java.time.LocalDate;

/**
 * Reorder levels report.
 * Requirement: "Reorder levels of stock. If any stock for a given item falls
 * below 50 items, that item should appear on the report."
 */
public class ReorderLevelsReport extends ReportTemplate {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LocalDate filterDate;
    private static final int REORDER_THRESHOLD = 50;

    public ReorderLevelsReport(InventoryRepository inventoryRepository, ProductRepository productRepository, LocalDate filterDate) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.filterDate = filterDate;
    }

    @Override
    protected String getReportHeader() {
        String dateHeader = filterDate != null ? "║               Date: " + String.format("%-55s", filterDate.toString()) + "║\n" : "";
        return "╔═══════════════════════════════════════════════════════════════════════════╗\n" +
                "║                        REORDER LEVELS REPORT                              ║\n" +
                dateHeader +
                "║               Items Below Reorder Level (" + REORDER_THRESHOLD + " units)                        ║\n"
                +
                "╚═══════════════════════════════════════════════════════════════════════════╝\n";
    }

    @Override
    protected String getReportBody() {
        List<Inventory> lowStockItems = inventoryRepository.findBelowReorderLevel();

        if (lowStockItems.isEmpty()) {
            return "\n✓ All items are above reorder level. No reorder required.\n";
        }

        StringBuilder body = new StringBuilder();
        body.append("\n");
        body.append(String.format("%-10s %-20s %-10s %-10s %-10s %-10s %-10s\n",
                "ITEM CODE", "PRODUCT NAME", "SHELF", "STORE", "ONLINE", "TOTAL", "REORDER"));
        body.append("─".repeat(80)).append("\n");

        for (Inventory inventory : lowStockItems) {
            String productName = productRepository.findByCode(inventory.getProductCode())
                    .map(Product::getName)
                    .orElse("Unknown");

            int totalQty = inventory.getTotalQuantity();
            int reorderQty = REORDER_THRESHOLD - totalQty + 20; // Reorder to bring above threshold + buffer

            body.append(String.format("%-10s %-20s %-10d %-10d %-10d %-10d %-10d\n",
                    inventory.getProductCode(),
                    productName.length() > 18 ? productName.substring(0, 18) : productName,
                    inventory.getShelfQuantity(),
                    inventory.getStoreQuantity(),
                    inventory.getOnlineQuantity(),
                    totalQty,
                    reorderQty));
        }

        body.append("─".repeat(80)).append("\n");
        body.append(String.format("\n⚠ Total items requiring reorder: %d\n", lowStockItems.size()));
        body.append("  Reorder threshold: " + REORDER_THRESHOLD + " units\n");

        return body.toString();
    }
}
