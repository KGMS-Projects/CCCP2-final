package com.syos.usecases.observers;

import com.syos.entities.Inventory;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject for inventory observations.
 * Part of Observer Pattern implementation.
 */
public class InventorySubject {
    private final List<InventoryObserver> observers = new ArrayList<>();

    public void attach(InventoryObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(InventoryObserver observer) {
        observers.remove(observer);
    }

    public void notifyInventoryChanged(Inventory inventory) {
        for (InventoryObserver observer : observers) {
            observer.onInventoryChanged(inventory);
        }

        // Check for low stock
        if (inventory.isBelowReorderLevel()) {
            notifyLowStock(inventory);
        }
    }

    private void notifyLowStock(Inventory inventory) {
        for (InventoryObserver observer : observers) {
            observer.onLowStock(inventory);
        }
    }

    public void notifyObservers(Inventory inv) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notifyObservers'");
    }
}
