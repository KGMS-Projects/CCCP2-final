package com.syos.usecases.repositories;

import com.syos.entities.Inventory;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Inventory data access.
 */
public interface InventoryRepository {
    void save(Inventory inventory);

    Optional<Inventory> findByProductCode(String productCode);

    List<Inventory> findAll();

    void update(Inventory inventory);

    List<Inventory> findBelowReorderLevel();
}
