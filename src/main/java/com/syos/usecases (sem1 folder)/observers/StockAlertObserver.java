package com.syos.usecases.observers;

import com.syos.entities.Inventory;

/**
 * Concrete observer that logs inventory changes.
 * Implements Observer Pattern.
 */
public class StockAlertObserver implements InventoryObserver {

    @Override
    public void onInventoryChanged(Inventory inventory) {
        System.out.println("[INFO] Inventory updated for product: " + inventory.getProductCode() +
                " | Total: " + inventory.getTotalQuantity());
    }

    @Override
    public void onLowStock(Inventory inventory) {
        System.out.println("[ALERT] Low stock for product: " + inventory.getProductCode() +
                " | Current: " + inventory.getTotalQuantity() +
                " | Reorder required!");
    }
}
