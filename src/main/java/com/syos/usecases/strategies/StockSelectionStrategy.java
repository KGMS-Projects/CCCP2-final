package com.syos.usecases.strategies;

import com.syos.entities.StockBatch;
import java.util.List;

/**
 * Strategy interface for selecting stock batches.
 * Implements Strategy Pattern - allows different algorithms for stock
 * selection.
 * Follows Open/Closed Principle - open for extension, closed for modification.
 */
public interface StockSelectionStrategy {
    /**
     * Selects the appropriate stock batch from available batches.
     * 
     * @param batches List of available stock batches
     * @return Selected stock batch, or null if none suitable
     */
    StockBatch selectBatch(List<StockBatch> batches);
}
