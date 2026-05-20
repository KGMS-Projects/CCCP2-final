package com.syos.usecases;

import com.syos.entities.Inventory;
import com.syos.entities.StockBatch;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.StockBatchRepository;
import com.syos.usecases.strategies.StockSelectionStrategy;

import java.util.List;

/**
 * Use case for managing stock transfers from store to shelf.
 * Implements the requirement: "Items are moved to the shelf from the store"
 */
public class TransferStockUseCase {
    private final InventoryRepository inventoryRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockSelectionStrategy stockSelectionStrategy;
    private final InventorySubject inventorySubject;

    public TransferStockUseCase(InventoryRepository inventoryRepository,
            StockBatchRepository stockBatchRepository,
            StockSelectionStrategy stockSelectionStrategy,
            InventorySubject inventorySubject) {
        this.inventoryRepository = inventoryRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.stockSelectionStrategy = stockSelectionStrategy;
        this.inventorySubject = inventorySubject;
    }

    public void execute(String productCode, int quantity, TransferType transferType) throws TransferException {
        Inventory inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new TransferException("Inventory not found for product: " + productCode));

        if (inventory.getStoreQuantity() < quantity) {
            throw new TransferException("Insufficient store quantity. Available: " +
                    inventory.getStoreQuantity() + ", Requested: " + quantity);
        }

        // Get batches for this product and reduce from selected batches using expiry
        // priority
        List<StockBatch> batches = stockBatchRepository.findByProductCode(productCode);
        int remainingQuantity = quantity;

        while (remainingQuantity > 0) {
            StockBatch selectedBatch = stockSelectionStrategy.selectBatch(batches);
            if (selectedBatch == null) {
                throw new TransferException("No available batches for product: " + productCode);
            }

            int reduceAmount = Math.min(remainingQuantity, selectedBatch.getQuantity());
            selectedBatch.reduceQuantity(reduceAmount);
            stockBatchRepository.update(selectedBatch);
            remainingQuantity -= reduceAmount;

            System.out.println("[BATCH] Reduced " + reduceAmount + " from batch: " + selectedBatch.getBatchId() +
                    " (Expiry: " + selectedBatch.getExpiryDate() + ")");
        }

        // Transfer based on type
        switch (transferType) {
            case STORE_TO_SHELF:
                inventory.transferFromStoreToShelf(quantity);
                break;
            case STORE_TO_ONLINE:
                inventory.transferFromStoreToOnline(quantity);
                break;
            default:
                throw new TransferException("Unknown transfer type: " + transferType);
        }

        inventoryRepository.update(inventory);
        inventorySubject.notifyInventoryChanged(inventory);
    }

    public enum TransferType {
        STORE_TO_SHELF,
        STORE_TO_ONLINE
    }

    public static class TransferException extends Exception {
        public TransferException(String message) {
            super(message);
        }
    }
}
