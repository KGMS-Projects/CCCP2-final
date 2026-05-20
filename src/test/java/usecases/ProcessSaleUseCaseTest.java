package usecases;

import com.syos.entities.Bill;
import com.syos.entities.Inventory;
import com.syos.entities.Product;
import com.syos.entities.StockBatch;
import com.syos.usecases.ProcessSaleUseCase;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.repositories.BillRepository;
import com.syos.usecases.repositories.InventoryRepository;
import com.syos.usecases.repositories.ProductRepository;
import com.syos.usecases.repositories.StockBatchRepository;
import com.syos.usecases.strategies.StockSelectionStrategy;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Process Sale Use Case Tests")
class ProcessSaleUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @Mock
    private StockSelectionStrategy stockSelectionStrategy;

    @Mock
    private InventorySubject inventorySubject;

    private ProcessSaleUseCase processSaleUseCase;

    @BeforeEach
    void setUp() {
        processSaleUseCase = new ProcessSaleUseCase(
                productRepository,
                billRepository,
                inventoryRepository,
                stockBatchRepository,
                stockSelectionStrategy,
                inventorySubject);
    }

    // ==================== HELPER METHODS ====================

    private Product createTestProduct(String code, String name, double price) {
        return new Product.Builder()
                .code(code)
                .name(name)
                .price(price)
                .build();
    }

    private Inventory createTestInventory(String productCode, int shelfQty, int onlineQty) {
        Inventory inventory = new Inventory(productCode);
        inventory.addToShelf(shelfQty);
        inventory.addToOnline(onlineQty);
        return inventory;
    }

    private StockBatch createTestBatch(String productCode, int quantity) {
        return new StockBatch(productCode, LocalDate.now(), quantity, LocalDate.now().plusDays(30));
    }

    private ProcessSaleUseCase.SaleRequest createSaleRequest(
            String productCode, int quantity, double cashTendered, Bill.TransactionType type) {
        List<ProcessSaleUseCase.SaleRequest.SaleItem> items = Arrays.asList(
                new ProcessSaleUseCase.SaleRequest.SaleItem(productCode, quantity));
        return new ProcessSaleUseCase.SaleRequest(items, cashTendered, type, null);
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("Should process counter sale successfully")
    void shouldProcessCounterSaleSuccessfully() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test Product", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 50);
        StockBatch batch = createTestBatch(productCode, 100);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.COUNTER);

        // ACT
        Bill result = processSaleUseCase.execute(request);

        // ASSERT
        assertNotNull(result);
        assertEquals(1, result.getSerialNumber());
        assertEquals(Bill.TransactionType.COUNTER, result.getTransactionType());
    }

    @Test
    @DisplayName("Should process online sale successfully")
    void shouldProcessOnlineSaleSuccessfully() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test Product", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 50);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(2);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.ONLINE);

        // ACT
        Bill result = processSaleUseCase.execute(request);

        // ASSERT
        assertNotNull(result);
        assertEquals(Bill.TransactionType.ONLINE, result.getTransactionType());
    }

    // ==================== VALIDATION ERROR TESTS ====================

    @Test
    @DisplayName("Should throw exception for null request")
    void shouldThrowExceptionForNullRequest() {
        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(null));
        assertEquals("Sale request cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for empty items list")
    void shouldThrowExceptionForEmptyItems() {
        ProcessSaleUseCase.SaleRequest request = new ProcessSaleUseCase.SaleRequest(
                Arrays.asList(), 100.00, Bill.TransactionType.COUNTER, null);

        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(request));
        assertEquals("Sale must have at least one item", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for negative cash tendered")
    void shouldThrowExceptionForNegativeCash() {
        ProcessSaleUseCase.SaleRequest request = createSaleRequest("P001", 5, -10.00,
                Bill.TransactionType.COUNTER);

        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(request));
        assertEquals("Cash tendered cannot be negative", exception.getMessage());
    }

    // ==================== PRODUCT/INVENTORY ERROR TESTS ====================

    @Test
    @DisplayName("Should throw exception for non-existent product")
    void shouldThrowExceptionForNonExistentProduct() {
        when(productRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                "INVALID", 5, 100.00, Bill.TransactionType.COUNTER);

        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(request));
        assertEquals("Product not found: INVALID", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for missing inventory")
    void shouldThrowExceptionForMissingInventory() {
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test", 10.00);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.empty());

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.COUNTER);

        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(request));
        assertTrue(exception.getMessage().contains("Inventory not found"));
    }

    @Test
    @DisplayName("Should throw exception for insufficient shelf stock")
    void shouldThrowExceptionForInsufficientShelfStock() {
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test Product", 10.00);
        Inventory inventory = createTestInventory(productCode, 3, 50); // Only 3 on shelf

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 10, 100.00, Bill.TransactionType.COUNTER // Requesting 10
        );

        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(request));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Should throw exception for insufficient online stock")
    void shouldThrowExceptionForInsufficientOnlineStock() {
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test Product", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 2); // Only 2 online

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 10, 100.00, Bill.TransactionType.ONLINE // Requesting 10
        );

        ProcessSaleUseCase.SaleException exception = assertThrows(
                ProcessSaleUseCase.SaleException.class,
                () -> processSaleUseCase.execute(request));
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    // ==================== REPOSITORY INTERACTION TESTS ====================

    @Test
    @DisplayName("Should save bill to repository")
    void shouldSaveBillToRepository() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 50);
        StockBatch batch = createTestBatch(productCode, 100);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.COUNTER);

        // ACT
        processSaleUseCase.execute(request);

        // ASSERT
        verify(billRepository).save(any(Bill.class));
    }

    @Test
    @DisplayName("Should update inventory after sale")
    void shouldUpdateInventoryAfterSale() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 50);
        StockBatch batch = createTestBatch(productCode, 100);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.COUNTER);

        // ACT
        processSaleUseCase.execute(request);

        // ASSERT
        verify(inventoryRepository).update(any(Inventory.class));
    }

    @Test
    @DisplayName("Should notify observers after inventory change")
    void shouldNotifyObserversAfterInventoryChange() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 50);
        StockBatch batch = createTestBatch(productCode, 100);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.COUNTER);

        // ACT
        processSaleUseCase.execute(request);

        // ASSERT
        verify(inventorySubject).notifyInventoryChanged(any(Inventory.class));
    }

    @Test
    @DisplayName("Should use stock selection strategy for counter sales")
    void shouldUseStockSelectionStrategy() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        Product product = createTestProduct(productCode, "Test", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, 50);
        StockBatch batch = createTestBatch(productCode, 100);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, 5, 100.00, Bill.TransactionType.COUNTER);

        // ACT
        processSaleUseCase.execute(request);

        // ASSERT - Strategy should be called for counter sales
        verify(stockSelectionStrategy).selectBatch(any());
    }

    // ==================== INVENTORY REDUCTION TESTS ====================

    @Test
    @DisplayName("Should reduce shelf quantity for counter sale")
    void shouldReduceShelfQuantityForCounterSale() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        int initialShelfQty = 100;
        int saleQty = 5;

        Product product = createTestProduct(productCode, "Test", 10.00);
        Inventory inventory = createTestInventory(productCode, initialShelfQty, 50);
        StockBatch batch = createTestBatch(productCode, 100);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, saleQty, 100.00, Bill.TransactionType.COUNTER);

        // ACT
        processSaleUseCase.execute(request);

        // ASSERT - Shelf quantity should be reduced
        assertEquals(initialShelfQty - saleQty, inventory.getShelfQuantity());
    }

    @Test
    @DisplayName("Should reduce online quantity for online sale")
    void shouldReduceOnlineQuantityForOnlineSale() throws ProcessSaleUseCase.SaleException {
        // ARRANGE
        String productCode = "P001";
        int initialOnlineQty = 50;
        int saleQty = 5;

        Product product = createTestProduct(productCode, "Test", 10.00);
        Inventory inventory = createTestInventory(productCode, 100, initialOnlineQty);

        when(productRepository.findByCode(productCode)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(billRepository.getNextSerialNumber()).thenReturn(1);

        ProcessSaleUseCase.SaleRequest request = createSaleRequest(
                productCode, saleQty, 100.00, Bill.TransactionType.ONLINE);

        // ACT
        processSaleUseCase.execute(request);

        // ASSERT - Online quantity should be reduced
        assertEquals(initialOnlineQty - saleQty, inventory.getOnlineQuantity());
    }
}
