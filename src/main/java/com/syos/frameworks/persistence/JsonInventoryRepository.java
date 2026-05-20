package com.syos.frameworks.persistence;

import com.google.gson.reflect.TypeToken;
import com.syos.entities.Inventory;
import com.syos.usecases.repositories.InventoryRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-based implementation of InventoryRepository.
 */
public class JsonInventoryRepository implements InventoryRepository {
    private static final String FILENAME = "inventory.json";
    private final JsonDataStore dataStore;
    private final Type listType = new TypeToken<ArrayList<InventoryData>>() {
    }.getType();

    public JsonInventoryRepository() {
        this.dataStore = JsonDataStore.getInstance();
    }

    @Override
    public void save(Inventory inventory) {
        List<InventoryData> inventories = loadAll();
        inventories.add(toData(inventory));
        dataStore.save(FILENAME, inventories);
    }

    @Override
    public Optional<Inventory> findByProductCode(String productCode) {
        return loadAll().stream()
                .filter(i -> i.productCode.equals(productCode))
                .map(this::toEntity)
                .findFirst();
    }

    @Override
    public List<Inventory> findAll() {
        return loadAll().stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public void update(Inventory inventory) {
        List<InventoryData> inventories = loadAll();

        for (int i = 0; i < inventories.size(); i++) {
            if (inventories.get(i).productCode.equals(inventory.getProductCode())) {
                inventories.set(i, toData(inventory));
                dataStore.save(FILENAME, inventories);
                return;
            }
        }

        throw new IllegalArgumentException("Inventory not found: " + inventory.getProductCode());
    }

    @Override
    public List<Inventory> findBelowReorderLevel() {
        return loadAll().stream()
                .map(this::toEntity)
                .filter(Inventory::isBelowReorderLevel)
                .toList();
    }

    private List<InventoryData> loadAll() {
        return dataStore.load(FILENAME, listType);
    }

    private InventoryData toData(Inventory inventory) {
        InventoryData data = new InventoryData();
        data.productCode = inventory.getProductCode();
        data.shelfQuantity = inventory.getShelfQuantity();
        data.storeQuantity = inventory.getStoreQuantity();
        data.onlineQuantity = inventory.getOnlineQuantity();
        return data;
    }

    private Inventory toEntity(InventoryData data) {
        Inventory inventory = new Inventory(data.productCode);

        if (data.shelfQuantity > 0) {
            inventory.addToShelf(data.shelfQuantity);
        }
        if (data.storeQuantity > 0) {
            inventory.addToStore(data.storeQuantity);
        }
        if (data.onlineQuantity > 0) {
            inventory.addToOnline(data.onlineQuantity);
        }

        return inventory;
    }

    private static class InventoryData {
        String productCode;
        int shelfQuantity;
        int storeQuantity;
        int onlineQuantity;
    }
}
