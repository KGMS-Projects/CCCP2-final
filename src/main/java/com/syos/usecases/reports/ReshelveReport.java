package com.syos.usecases.reports;

import com.syos.entities.Inventory;
import com.syos.entities.Product;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.ProductRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;

/**
 * Reshelve report implementation.
 * Requirement 4.b: "The total number of items with code, name, and quantity
 * must be reshelved at the end of the day."
 * 
 * This report identifies products that need to be transferred from the store
 * to the shelf to replenish shelf inventory.
 * 
 * Implements Template Method pattern by extending ReportTemplate.
 */
public class ReshelveReport extends ReportTemplate {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LocalDate filterDate;

    // Threshold: if shelf quantity is below 30% of total available, recommend
    // reshelving
    private static final double SHELF_THRESHOLD_PERCENTAGE = 0.30;

    public ReshelveReport(InventoryRepository inventoryRepository,
            ProductRepository productRepository, LocalDate filterDate) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.filterDate = filterDate;
    }

    @Override
    protected String getReportHeader() {
        String dateHeader = filterDate != null ? "For Date: " + filterDate.toString() + "\n" : "";
        return "=== RESHELVE REPORT ===\n" +
                dateHeader +
                "Items that need to be reshelved from store to shelf\n" +
                "Generated for end-of-day stock management\n";
    }

    @Override
    protected String getReportBody() {
        List<Inventory> inventories = inventoryRepository.findAll();

        if (inventories.isEmpty()) {
            return "No inventory data available.";
        }

        // Collect items that need reshelving
        List<ReshelveItem> reshelveItems = new ArrayList<>();

        for (Inventory inventory : inventories) {
            // Calculate if this item needs reshelving
            int shelfQty = inventory.getShelfQuantity();
            int storeQty = inventory.getStoreQuantity();
            int totalAvailable = shelfQty + storeQty;

            // Skip if no store stock available
            if (storeQty == 0) {
                continue;
            }

            // Calculate threshold (30% of total available)
            int threshold = (int) Math.ceil(totalAvailable * SHELF_THRESHOLD_PERCENTAGE);

            // If shelf quantity is below threshold, recommend reshelving
            if (shelfQty < threshold) {
                Product product = productRepository.findByCode(inventory.getProductCode())
                        .orElse(null);

                if (product != null) {
                    // Calculate recommended reshelve quantity
                    // Bring shelf up to threshold, but don't exceed available store quantity
                    int recommended = Math.min(threshold - shelfQty, storeQty);

                    reshelveItems.add(new ReshelveItem(
                            product.getCode(),
                            product.getName(),
                            shelfQty,
                            storeQty,
                            recommended));
                }
            }
        }

        if (reshelveItems.isEmpty()) {
            return "No items need reshelving at this time.\n" +
                    "All shelf quantities are adequately stocked.";
        }

        // Sort by product code
        reshelveItems.sort(Comparator.comparing(item -> item.code));

        // Build report
        StringBuilder body = new StringBuilder();
        body.append(String.format("%-10s %-30s %-12s %-12s %-15s\n",
                "Code", "Name", "Shelf Qty", "Store Qty", "Recommended"));
        body.append("-".repeat(85)).append("\n");

        int totalItemsToReshelve = 0;
        int totalQuantityToReshelve = 0;

        for (ReshelveItem item : reshelveItems) {
            body.append(String.format("%-10s %-30s %-12d %-12d %-15d\n",
                    item.code,
                    item.name,
                    item.shelfQuantity,
                    item.storeQuantity,
                    item.recommendedQuantity));

            totalItemsToReshelve++;
            totalQuantityToReshelve += item.recommendedQuantity;
        }

        body.append("-".repeat(85)).append("\n");
        body.append(String.format("Total Items to Reshelve: %d\n", totalItemsToReshelve));
        body.append(String.format("Total Quantity to Transfer: %d units\n", totalQuantityToReshelve));
        body.append("\nNote: Use Stock Management > Transfer Stock to Shelf to reshelve items.\n");

        return body.toString();
    }

    /**
     * Internal class to hold reshelve item data.
     */
    private static class ReshelveItem {
        String code;
        String name;
        int shelfQuantity;
        int storeQuantity;
        int recommendedQuantity;

        ReshelveItem(String code, String name, int shelfQuantity,
                int storeQuantity, int recommendedQuantity) {
            this.code = code;
            this.name = name;
            this.shelfQuantity = shelfQuantity;
            this.storeQuantity = storeQuantity;
            this.recommendedQuantity = recommendedQuantity;
        }
    }
}
