package usecases;

import com.syos.entities.Inventory;
import com.syos.entities.StockBatch;
import com.syos.usecases.TransferStockUseCase;
import com.syos.usecases.observers.InventorySubject;
import com.syos.usecases.repositories.InventoryRepository;
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
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transfer Stock Use Case Tests")
class TransferStockUseCaseTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @Mock
    private StockSelectionStrategy stockSelectionStrategy;

    @Mock
    private InventorySubject inventorySubject;

    private TransferStockUseCase transferStockUseCase;

    @BeforeEach
    void setUp() {
        transferStockUseCase = new TransferStockUseCase(
                inventoryRepository,
                stockBatchRepository,
                stockSelectionStrategy,
                inventorySubject);
    }

    // ==================== HELPER METHODS ====================

    private Inventory createTestInventory(String productCode, int storeQty, int shelfQty, int onlineQty) {
        Inventory inventory = new Inventory(productCode);
        inventory.addToStore(storeQty);
        inventory.addToShelf(shelfQty);
        inventory.addToOnline(onlineQty);
        return inventory;
    }

    private StockBatch createTestBatch(String productCode, int quantity) {
        return new StockBatch(productCode, LocalDate.now(), quantity, LocalDate.now().plusDays(30));
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("Should transfer stock from store to shelf successfully")
    void shouldTransferFromStoreToShelfSuccessfully() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        int transferQty = 20;
        int initialStoreQty = 100;
        int initialShelfQty = 50;

        Inventory inventory = createTestInventory(productCode, initialStoreQty, initialShelfQty, 0);
        StockBatch batch = createTestBatch(productCode, 100);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, transferQty, TransferStockUseCase.TransferType.STORE_TO_SHELF);

        // ASSERT
        assertEquals(initialStoreQty - transferQty, inventory.getStoreQuantity());
        assertEquals(initialShelfQty + transferQty, inventory.getShelfQuantity());
    }

    @Test
    @DisplayName("Should transfer stock from store to online successfully")
    void shouldTransferFromStoreToOnlineSuccessfully() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        int transferQty = 30;
        int initialStoreQty = 100;
        int initialOnlineQty = 20;

        Inventory inventory = createTestInventory(productCode, initialStoreQty, 0, initialOnlineQty);
        StockBatch batch = createTestBatch(productCode, 100);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, transferQty, TransferStockUseCase.TransferType.STORE_TO_ONLINE);

        // ASSERT
        assertEquals(initialStoreQty - transferQty, inventory.getStoreQuantity());
        assertEquals(initialOnlineQty + transferQty, inventory.getOnlineQuantity());
    }

    // ==================== ERROR TESTS ====================

    @Test
    @DisplayName("Should throw exception for non-existent inventory")
    void shouldThrowExceptionForNonExistentInventory() {
        when(inventoryRepository.findByProductCode("INVALID")).thenReturn(Optional.empty());

        TransferStockUseCase.TransferException exception = assertThrows(
                TransferStockUseCase.TransferException.class,
                () -> transferStockUseCase.execute("INVALID", 10, TransferStockUseCase.TransferType.STORE_TO_SHELF));

        assertTrue(exception.getMessage().contains("Inventory not found"));
    }

    @Test
    @DisplayName("Should throw exception for insufficient store quantity")
    void shouldThrowExceptionForInsufficientStoreQuantity() {
        String productCode = "P001";
        Inventory inventory = createTestInventory(productCode, 5, 0, 0); // Only 5 in store

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));

        TransferStockUseCase.TransferException exception = assertThrows(
                TransferStockUseCase.TransferException.class,
                () -> transferStockUseCase.execute(productCode, 20, TransferStockUseCase.TransferType.STORE_TO_SHELF));

        assertTrue(exception.getMessage().contains("Insufficient store quantity"));
    }

    @Test
    @DisplayName("Should throw exception when no batches available")
    void shouldThrowExceptionWhenNoBatchesAvailable() {
        String productCode = "P001";
        Inventory inventory = createTestInventory(productCode, 100, 0, 0);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList());
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(null); // No batch selected

        TransferStockUseCase.TransferException exception = assertThrows(
                TransferStockUseCase.TransferException.class,
                () -> transferStockUseCase.execute(productCode, 10, TransferStockUseCase.TransferType.STORE_TO_SHELF));

        assertTrue(exception.getMessage().contains("No available batches"));
    }

    // ==================== REPOSITORY INTERACTION TESTS ====================

    @Test
    @DisplayName("Should update inventory repository after transfer")
    void shouldUpdateInventoryRepository() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        Inventory inventory = createTestInventory(productCode, 100, 0, 0);
        StockBatch batch = createTestBatch(productCode, 100);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, 10, TransferStockUseCase.TransferType.STORE_TO_SHELF);

        // ASSERT
        verify(inventoryRepository).update(any(Inventory.class));
    }

    @Test
    @DisplayName("Should update stock batch repository after transfer")
    void shouldUpdateStockBatchRepository() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        Inventory inventory = createTestInventory(productCode, 100, 0, 0);
        StockBatch batch = createTestBatch(productCode, 100);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, 10, TransferStockUseCase.TransferType.STORE_TO_SHELF);

        // ASSERT
        verify(stockBatchRepository).update(any(StockBatch.class));
    }

    @Test
    @DisplayName("Should notify observers after inventory change")
    void shouldNotifyObserversAfterInventoryChange() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        Inventory inventory = createTestInventory(productCode, 100, 0, 0);
        StockBatch batch = createTestBatch(productCode, 100);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, 10, TransferStockUseCase.TransferType.STORE_TO_SHELF);

        // ASSERT
        verify(inventorySubject).notifyInventoryChanged(any(Inventory.class));
    }

    @Test
    @DisplayName("Should use stock selection strategy")
    void shouldUseStockSelectionStrategy() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        Inventory inventory = createTestInventory(productCode, 100, 0, 0);
        StockBatch batch = createTestBatch(productCode, 100);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, 10, TransferStockUseCase.TransferType.STORE_TO_SHELF);

        // ASSERT
        verify(stockSelectionStrategy).selectBatch(any());
    }

    // ==================== BATCH REDUCTION TESTS ====================

    @Test
    @DisplayName("Should reduce batch quantity during transfer")
    void shouldReduceBatchQuantityDuringTransfer() throws TransferStockUseCase.TransferException {
        // ARRANGE
        String productCode = "P001";
        int initialBatchQty = 100;
        int transferQty = 25;

        Inventory inventory = createTestInventory(productCode, 100, 0, 0);
        StockBatch batch = createTestBatch(productCode, initialBatchQty);

        when(inventoryRepository.findByProductCode(productCode)).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductCode(productCode)).thenReturn(Arrays.asList(batch));
        when(stockSelectionStrategy.selectBatch(any())).thenReturn(batch);

        // ACT
        transferStockUseCase.execute(productCode, transferQty, TransferStockUseCase.TransferType.STORE_TO_SHELF);

        // ASSERT
        assertEquals(initialBatchQty - transferQty, batch.getQuantity());
    }
}

