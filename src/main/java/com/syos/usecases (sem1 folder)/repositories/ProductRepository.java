package com.syos.usecases.repositories;

import com.syos.entities.Product;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Product data access.
 * Follows Dependency Inversion Principle - high-level modules depend on
 * abstractions.
 * Follows Interface Segregation Principle - specific interface for product
 * operations.
 */
public interface ProductRepository {
    void save(Product product);

    Optional<Product> findByCode(String code);

    List<Product> findAll();

    void update(Product product);

    void delete(String code);

    boolean exists(String code);
}
