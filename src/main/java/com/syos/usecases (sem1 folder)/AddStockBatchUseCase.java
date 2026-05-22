package com.syos.usecases;

import com.syos.entities.Inventory;
import com.syos.entities.Product;
import com.syos.entities.StockBatch;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.ProductRepository;
import com.syos.usecases.repositories.StockBatchRepository;

import java.time.LocalDate;

/**
 * Use case for adding new stock batches.
 * Implements requirement: "Items are first bought at the SYOS store and are
 * stocked
 * according to the code, date of purchase, amount of quantity received, and
 * date of expiry"
 */
public class AddStockBatchUseCase {
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventorySubject inventorySubject;

    public AddStockBatchUseCase(ProductRepository productRepository,
            StockBatchRepository stockBatchRepository,
            InventoryRepository inventoryRepository,
            InventorySubject inventorySubject) {
        this.productRepository = productRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventorySubject = inventorySubject;
    }

    public StockBatch execute(String productCode, int quantity, LocalDate expiryDate) throws StockException {
        // Validate product exists
        Product product = productRepository.findByCode(productCode)
                .orElseThrow(() -> new StockException("Product not found: " + productCode));

        // Create stock batch
        StockBatch stockBatch = new StockBatch(productCode, LocalDate.now(), quantity, expiryDate);
        stockBatchRepository.save(stockBatch);

        // Update inventory
        Inventory inventory = inventoryRepository.findByProductCode(productCode)
                .orElseGet(() -> new Inventory(productCode));

        inventory.addToStore(quantity);

        if (inventoryRepository.findByProductCode(productCode).isPresent()) {
            inventoryRepository.update(inventory);
        } else {
            inventoryRepository.save(inventory);
        }

        inventorySubject.notifyInventoryChanged(inventory);

        return stockBatch;
    }

    public static class StockException extends Exception {
        public StockException(String message) {
            super(message);
        }
    }
}
