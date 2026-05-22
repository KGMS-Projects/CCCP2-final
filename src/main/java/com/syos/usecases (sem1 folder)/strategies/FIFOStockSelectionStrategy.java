package com.syos.usecases.strategies;

import com.syos.entities.StockBatch;
import java.util.Comparator;
import java.util.List;

/**
 * FIFO (First In, First Out) stock selection strategy.
 * Selects the oldest batch first.
 */
public class FIFOStockSelectionStrategy implements StockSelectionStrategy {

    @Override
    public StockBatch selectBatch(List<StockBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            return null;
        }

        return batches.stream()
                .filter(batch -> batch.getQuantity() > 0)
                .filter(batch -> !batch.isExpired())
                .min(Comparator.comparing(StockBatch::getPurchaseDate))
                .orElse(null);
    }
}
