package com.syos.usecases.repositories;

import com.syos.entities.StockBatch;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StockBatch data access.
 */
public interface StockBatchRepository {
    void save(StockBatch stockBatch);

    Optional<StockBatch> findById(String batchId);

    List<StockBatch> findByProductCode(String productCode);

    List<StockBatch> findAll();

    void update(StockBatch stockBatch);

    void delete(String batchId);
}
