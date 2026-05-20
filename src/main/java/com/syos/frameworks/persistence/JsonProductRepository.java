package com.syos.frameworks.persistence;

import com.google.gson.reflect.TypeToken;
import com.syos.entities.Product;
import com.syos.usecases.repositories.ProductRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-based implementation of ProductRepository.
 * Implements Repository Pattern for data access abstraction.
 */
public class JsonProductRepository implements ProductRepository {
    private static final String FILENAME = "products.json";
    private final JsonDataStore dataStore;
    private final Type listType = new TypeToken<ArrayList<ProductData>>() {
    }.getType();

    public JsonProductRepository() {
        this.dataStore = JsonDataStore.getInstance();
    }

    @Override
    public void save(Product product) {
        List<ProductData> products = loadAll();

        // Check if product already exists
        boolean exists = products.stream()
                .anyMatch(p -> p.code.equals(product.getCode()));

        if (exists) {
            throw new IllegalArgumentException("Product with code " + product.getCode() + " already exists");
        }

        products.add(toData(product));
        dataStore.save(FILENAME, products);
    }

    @Override
    public Optional<Product> findByCode(String code) {
        return loadAll().stream()
                .filter(p -> p.code.equals(code))
                .map(this::toEntity)
                .findFirst();
    }

    @Override
    public List<Product> findAll() {
        return loadAll().stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public void update(Product product) {
        List<ProductData> products = loadAll();

        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).code.equals(product.getCode())) {
                products.set(i, toData(product));
                dataStore.save(FILENAME, products);
                return;
            }
        }

        throw new IllegalArgumentException("Product not found: " + product.getCode());
    }

    @Override
    public void delete(String code) {
        List<ProductData> products = loadAll();
        products.removeIf(p -> p.code.equals(code));
        dataStore.save(FILENAME, products);
    }

    @Override
    public boolean exists(String code) {
        return loadAll().stream()
                .anyMatch(p -> p.code.equals(code));
    }

    private List<ProductData> loadAll() {
        return dataStore.load(FILENAME, listType);
    }

    private ProductData toData(Product product) {
        ProductData data = new ProductData();
        data.code = product.getCode();
        data.name = product.getName();
        data.price = product.getPrice();
        data.discountPercentage = product.getDiscountPercentage();
        return data;
    }

    private Product toEntity(ProductData data) {
        return new Product.Builder()
                .code(data.code)
                .name(data.name)
                .price(data.price)
                .discountPercentage(data.discountPercentage)
                .build();
    }

    private static class ProductData {
        String code;
        String name;
        double price;
        double discountPercentage;
    }
}
