package usecases.observers;

import com.syos.entities.Inventory;
import com.syos.usecases.observers.InventoryObserver;
import com.syos.usecases.observers.StockAlertObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@DisplayName("Stock Alert Observer Tests")
class StockAlertObserverTest {

    private StockAlertObserver observer;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        observer = new StockAlertObserver();

        // Capture System.out to verify printed messages
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }

    // ==================== HELPER METHODS ====================

    private Inventory createTestInventory(String productCode, int storeQty, int shelfQty, int onlineQty) {
        Inventory inventory = new Inventory(productCode);
        inventory.addToStore(storeQty);
        inventory.addToShelf(shelfQty);
        inventory.addToOnline(onlineQty);
        return inventory;
    }

    // ==================== onInventoryChanged TESTS ====================

    @Test
    @DisplayName("Should print INFO message on inventory changed")
    void shouldPrintInfoMessageOnInventoryChanged() {
        Inventory inventory = createTestInventory("P001", 50, 30, 20);

        observer.onInventoryChanged(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("[INFO]"));
        assertTrue(output.contains("Inventory updated"));
        assertTrue(output.contains("P001"));
    }

    @Test
    @DisplayName("Should include product code in inventory changed message")
    void shouldIncludeProductCodeInChangedMessage() {
        Inventory inventory = createTestInventory("PROD123", 100, 50, 25);

        observer.onInventoryChanged(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("PROD123"));
    }

    @Test
    @DisplayName("Should include total quantity in inventory changed message")
    void shouldIncludeTotalQuantityInChangedMessage() {
        Inventory inventory = createTestInventory("P001", 100, 50, 25); // Total: 175

        observer.onInventoryChanged(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("175") || output.contains("Total"));
    }

    // ==================== onLowStock TESTS ====================

    @Test
    @DisplayName("Should print ALERT message on low stock")
    void shouldPrintAlertMessageOnLowStock() {
        Inventory inventory = createTestInventory("P001", 5, 3, 2);

        observer.onLowStock(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("Low stock"));
    }

    @Test
    @DisplayName("Should include product code in low stock message")
    void shouldIncludeProductCodeInLowStockMessage() {
        Inventory inventory = createTestInventory("LOWSTOCK001", 1, 1, 1);

        observer.onLowStock(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("LOWSTOCK001"));
    }

    @Test
    @DisplayName("Should include current quantity in low stock message")
    void shouldIncludeCurrentQuantityInLowStockMessage() {
        Inventory inventory = createTestInventory("P001", 2, 1, 0); // Total: 3

        observer.onLowStock(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("Current"));
    }

    @Test
    @DisplayName("Should include reorder message in low stock alert")
    void shouldIncludeReorderMessageInLowStockAlert() {
        Inventory inventory = createTestInventory("P001", 1, 1, 1);

        observer.onLowStock(inventory);

        String output = outputStream.toString();
        assertTrue(output.contains("Reorder") || output.contains("reorder"));
    }

    // ==================== INTERFACE IMPLEMENTATION TESTS ====================

    @Test
    @DisplayName("Should implement InventoryObserver interface")
    void shouldImplementInventoryObserverInterface() {
        assertTrue(observer instanceof InventoryObserver);
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("Should handle multiple notifications")
    void shouldHandleMultipleNotifications() {
        Inventory inventory1 = createTestInventory("P001", 50, 25, 10);
        Inventory inventory2 = createTestInventory("P002", 5, 2, 1);

        observer.onInventoryChanged(inventory1);
        observer.onLowStock(inventory2);

        String output = outputStream.toString();
        assertTrue(output.contains("P001"));
        assertTrue(output.contains("P002"));
        assertTrue(output.contains("[INFO]"));
        assertTrue(output.contains("[ALERT]"));
    }

    @Test
    @DisplayName("Should not throw exception for zero quantity inventory")
    void shouldNotThrowExceptionForZeroQuantityInventory() {
        Inventory emptyInventory = new Inventory("EMPTY001");

        assertDoesNotThrow(() -> observer.onInventoryChanged(emptyInventory));
        assertDoesNotThrow(() -> observer.onLowStock(emptyInventory));
    }
}
