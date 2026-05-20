package usecases;

import com.syos.entities.Inventory;
import com.syos.entities.Product;
import com.syos.entities.StockBatch;
import com.syos.usecases.AddStockBatchUseCase;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.ProductRepository;
import com.syos.usecases.repositories.StockBatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;


import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Add Stock Batch Use Case Tests")
class AddStockBatchUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventorySubject inventorySubject;

    private AddStockBatchUseCase addStockBatchUseCase;

    @BeforeEach
    void setUp() {
        addStockBatchUseCase = new AddStockBatchUseCase(
                productRepository,
                stockBatchRepository,
                inventoryRepository,
                inventorySubject);
    }

    // ==================== HELPER METHODS ====================

    private Product createTestProduct(String code) {
        return new Product.Builder()
                .code(code)
                .name("Test Product")
                .price(10.00)
                .build();
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("Should add stock batch successfully for existing product")
    void shouldAddStockBatchSuccessfully() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P001";
        int quantity = 100;
        LocalDate expiryDate = LocalDate.now().plusDays(30);

        Product product = createTestProduct(productCode);
        Inventory existingInventory = new Inventory(productCode);
        existingInventory.addToStore(50); // Already has 50 in store

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(existingInventory));

        // ACT
        StockBatch result = addStockBatchUseCase.execute(productCode, quantity, expiryDate);

        // ASSERT
        assertNotNull(result);
        assertEquals(productCode, result.getProductCode());
        assertEquals(quantity, result.getQuantity());
        assertEquals(expiryDate, result.getExpiryDate());
    }

    @Test
    @DisplayName("Should create new inventory for new product")
    void shouldCreateNewInventoryForNewProduct() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P002";
        Product product = createTestProduct(productCode);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        // First call returns empty (for orElseGet), ensures inventory.save is called
        when(inventoryRepository.findByProductCode(productCode))
                .thenReturn(Optional.empty()) // First call - for orElseGet
                .thenReturn(Optional.empty()); // Second call - for the if check

        // ACT
        addStockBatchUseCase.execute(productCode, 100, LocalDate.now().plusDays(30));

        // ASSERT - save should be called (not update) for new inventory
        verify(inventoryRepository).save(any(Inventory.class));
        verify(inventoryRepository, never()).update(any(Inventory.class));
    }

    @Test
    @DisplayName("Should update existing inventory when adding stock")
    void shouldUpdateExistingInventory() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode);
        Inventory existingInventory = new Inventory(productCode);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(existingInventory));

        // ACT
        addStockBatchUseCase.execute(productCode, 100, LocalDate.now().plusDays(30));

        // ASSERT - update should be called (not save) for existing inventory
        verify(inventoryRepository).update(any(Inventory.class));
    }

    // ==================== ERROR CASES ====================

    @Test
    @DisplayName("Should throw exception for non-existent product")
    void shouldThrowExceptionForNonExistentProduct() {
        // ARRANGE
        when(productRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // ACT & ASSERT
        AddStockBatchUseCase.StockException exception = assertThrows(
                AddStockBatchUseCase.StockException.class,
                () -> addStockBatchUseCase.execute("INVALID", 100, LocalDate.now().plusDays(30)));

        assertEquals("Product not found: INVALID", exception.getMessage());

        // Verify no stock batch was saved
        verify(stockBatchRepository, never()).save(any(StockBatch.class));
    }

    // ==================== REPOSITORY INTERACTION TESTS ====================

    @Test
    @DisplayName("Should save stock batch to repository")
    void shouldSaveStockBatchToRepository() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode);
        Inventory inventory = new Inventory(productCode);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        // ACT
        addStockBatchUseCase.execute(productCode, 100, LocalDate.now().plusDays(30));

        // ASSERT
        verify(stockBatchRepository).save(any(StockBatch.class));
    }

    @Test
    @DisplayName("Should notify observers after inventory change")
    void shouldNotifyObserversAfterInventoryChange() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode);
        Inventory inventory = new Inventory(productCode);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        // ACT
        addStockBatchUseCase.execute(productCode, 100, LocalDate.now().plusDays(30));

        // ASSERT - Observer should be notified
        verify(inventorySubject).notifyInventoryChanged(any(Inventory.class));
    }

    @Test
    @DisplayName("Should check product exists before creating batch")
    void shouldCheckProductExistsFirst() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode);
        Inventory inventory = new Inventory(productCode);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        // ACT
        addStockBatchUseCase.execute(productCode, 100, LocalDate.now().plusDays(30));

        // ASSERT - Product repository should be queried
        verify(productRepository).findByCode(productCode);
    }

    // ==================== INVENTORY QUANTITY TESTS ====================

    @Test
    @DisplayName("Should add quantity to store inventory")
    void shouldAddQuantityToStoreInventory() throws AddStockBatchUseCase.StockException {
        // ARRANGE
        String productCode = "P001";
        int initialQuantity = 50;
        int addedQuantity = 100;

        Product product = createTestProduct(productCode);
        Inventory inventory = new Inventory(productCode);
        inventory.addToStore(initialQuantity);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        // ACT
        addStockBatchUseCase.execute(productCode, addedQuantity, LocalDate.now().plusDays(30));

        // ASSERT - Inventory should have original + new quantity
        assertEquals(initialQuantity + addedQuantity, inventory.getStoreQuantity());
    }
}
