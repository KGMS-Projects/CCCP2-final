package com.syos.frameworks.persistence;

import com.google.gson.reflect.TypeToken;
import com.syos.entities.StockBatch;
import com.syos.usecases.repositories.StockBatchRepository;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-based implementation of StockBatchRepository.
 */
public class JsonStockBatchRepository implements StockBatchRepository {
    private static final String FILENAME = "stock_batches.json";
    private final JsonDataStore dataStore;
    private final Type listType = new TypeToken<ArrayList<StockBatchData>>() {
    }.getType();

    public JsonStockBatchRepository() {
        this.dataStore = JsonDataStore.getInstance();
    }

    @Override
    public void save(StockBatch stockBatch) {
        List<StockBatchData> batches = loadAll();
        batches.add(toData(stockBatch));
        dataStore.save(FILENAME, batches);
    }

    @Override
    public Optional<StockBatch> findById(String batchId) {
        return loadAll().stream()
                .filter(b -> b.batchId.equals(batchId))
                .map(this::toEntity)
                .findFirst();
    }

    @Override
    public List<StockBatch> findByProductCode(String productCode) {
        return loadAll().stream()
                .filter(b -> b.productCode.equals(productCode))
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<StockBatch> findAll() {
        return loadAll().stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public void update(StockBatch stockBatch) {
        List<StockBatchData> batches = loadAll();

        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).batchId.equals(stockBatch.getBatchId())) {
                batches.set(i, toData(stockBatch));
                dataStore.save(FILENAME, batches);
                return;
            }
        }

        throw new IllegalArgumentException("Stock batch not found: " + stockBatch.getBatchId());
    }

    @Override
    public void delete(String batchId) {
        List<StockBatchData> batches = loadAll();
        batches.removeIf(b -> b.batchId.equals(batchId));
        dataStore.save(FILENAME, batches);
    }

    private List<StockBatchData> loadAll() {
        return dataStore.load(FILENAME, listType);
    }

    private StockBatchData toData(StockBatch batch) {
        StockBatchData data = new StockBatchData();
        data.batchId = batch.getBatchId();
        data.productCode = batch.getProductCode();
        data.purchaseDate = batch.getPurchaseDate().toString();
        data.quantity = batch.getQuantity();
        data.expiryDate = batch.getExpiryDate().toString();
        return data;
    }

    private StockBatch toEntity(StockBatchData data) {
        StockBatch batch = new StockBatch(
                data.productCode,
                LocalDate.parse(data.purchaseDate),
                data.quantity,
                LocalDate.parse(data.expiryDate));

        int difference = batch.getQuantity() - data.quantity;
        if (difference > 0) {
            batch.reduceQuantity(difference);
        }

        return batch;
    }

    private static class StockBatchData {
        String batchId;
        String productCode;
        String purchaseDate;
        int quantity;
        String expiryDate;
    }
}
