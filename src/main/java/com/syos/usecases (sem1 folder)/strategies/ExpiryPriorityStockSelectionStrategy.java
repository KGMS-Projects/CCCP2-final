package com.syos.usecases.strategies;

import com.syos.entities.StockBatch;
import java.util.Comparator;
import java.util.List;

/**
 * Expiry-priority stock selection strategy.
 * Selects batches based on expiry date priority:
 * - If a newer batch has an expiry date closer than the oldest batch, select
 * the newer batch
 * - Otherwise, use FIFO (oldest first)
 * 
 * This implements the requirement: "stock should be reduced from the oldest
 * batch of items
 * and put on the shelf. However, when the expiry date of another set is closer
 * than the one
 * in the oldest batch of items, the newer batch is chosen"
 */
public class ExpiryPriorityStockSelectionStrategy implements StockSelectionStrategy {

    private static final int EXPIRY_THRESHOLD_DAYS = 30; // Consider batches expiring within 30 days as priority

    @Override
    public StockBatch selectBatch(List<StockBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }

        // Filter out expired and empty batches
        List<StockBatch> availableBatches = batches.stream()
                .filter(batch -> batch.getQuantity() > 0)
                .filter(batch -> !batch.isExpired())
                .toList();

        if (availableBatches.isEmpty()) {
            return null;
        }

        // Find the oldest batch (FIFO)
        StockBatch oldestBatch = availableBatches.stream()
                .min(Comparator.comparing(StockBatch::getPurchaseDate))
                .orElse(null);

        if (oldestBatch == null) {
            return null;
        }

        // Check if any other batch has a closer expiry date
        StockBatch closestExpiryBatch = availableBatches.stream()
                .min(Comparator.comparing(StockBatch::getExpiryDate))
                .orElse(null);

        // If the closest expiry batch is different from oldest and expires sooner,
        // select it
        if (closestExpiryBatch != null &&
                !closestExpiryBatch.equals(oldestBatch) &&
                closestExpiryBatch.getExpiryDate().isBefore(oldestBatch.getExpiryDate())) {
            return closestExpiryBatch;
        }

        // Otherwise, use FIFO
        return oldestBatch;
    }
}
