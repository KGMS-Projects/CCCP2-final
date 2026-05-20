package com.syos.usecases.observers;

import com.syos.entities.Inventory;

/**
 * Observer interface for inventory changes.
 * Implements Observer Pattern - allows objects to be notified of inventory
 * changes.
 */
public interface InventoryObserver {
    void onInventoryChanged(Inventory inventory);

    void onLowStock(Inventory inventory);
}
