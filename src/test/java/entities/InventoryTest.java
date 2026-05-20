package entities;

import com.syos.entities.Inventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Inventory Entity Tests")
class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        // This runs before EACH test - gives us a fresh inventory
        inventory = new Inventory("P001");
    }

    //Test Basic Creation
    @Test
    @DisplayName("Should create inventory with zero quantities")
    void shouldCreateInventoryWithZeroQuantities() {
        assertEquals("P001", inventory.getProductCode());
        assertEquals(0, inventory.getShelfQuantity());
        assertEquals(0, inventory.getStoreQuantity());
        assertEquals(0, inventory.getOnlineQuantity());
        assertEquals(0, inventory.getTotalQuantity());
    }

    @Test
    @DisplayName("Should throw exception for empty product code")
    void shouldThrowExceptionForEmptyProductCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Inventory("");
        });
    }

    //Test Add Methods
    @Test
    @DisplayName("Should add to shelf quantity")
    void shouldAddToShelfQuantity() {
        inventory.addToShelf(50);

        assertEquals(50, inventory.getShelfQuantity());
        assertEquals(50, inventory.getTotalQuantity());
    }

    @Test
    @DisplayName("Should add to store quantity")
    void shouldAddToStoreQuantity() {
        inventory.addToStore(100);

        assertEquals(100, inventory.getStoreQuantity());
    }

    @Test
    @DisplayName("Should add to online quantity")
    void shouldAddToOnlineQuantity() {
        inventory.addToOnline(30);

        assertEquals(30, inventory.getOnlineQuantity());
    }

    @Test
    @DisplayName("Should throw exception when adding zero or negative")
    void shouldThrowExceptionWhenAddingZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            inventory.addToShelf(0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            inventory.addToShelf(-5);
        });
    }

    //Test Reduce Methods
    @Test
    @DisplayName("Should reduce from shelf quantity")
    void shouldReduceFromShelfQuantity() {
        inventory.addToShelf(100);
        inventory.reduceFromShelf(30);

        assertEquals(70, inventory.getShelfQuantity());
    }

    @Test
    @DisplayName("Should throw exception when reducing more than available")
    void shouldThrowExceptionWhenReducingMoreThanAvailable() {
        inventory.addToShelf(10);

        assertThrows(IllegalArgumentException.class, () -> {
            inventory.reduceFromShelf(20);  // Only 10 available
        });
    }

    //Test Transfer Methods
    @Test
    @DisplayName("Should transfer from store to shelf")
    void shouldTransferFromStoreToShelf() {
        inventory.addToStore(100);
        inventory.transferFromStoreToShelf(40);

        assertEquals(60, inventory.getStoreQuantity());
        assertEquals(40, inventory.getShelfQuantity());
    }

    @Test
    @DisplayName("Should transfer from store to online")
    void shouldTransferFromStoreToOnline() {
        inventory.addToStore(100);
        inventory.transferFromStoreToOnline(25);

        assertEquals(75, inventory.getStoreQuantity());
        assertEquals(25, inventory.getOnlineQuantity());
    }

    //Test Reorder Level
    @Test
    @DisplayName("Should be below reorder level when total < 50")
    void shouldBeBelowReorderLevel() {
        inventory.addToStore(30);

        assertTrue(inventory.isBelowReorderLevel());
    }

    @Test
    @DisplayName("Should NOT be below reorder level when total >= 50")
    void shouldNotBeBelowReorderLevel() {
        inventory.addToStore(50);

        assertFalse(inventory.isBelowReorderLevel());
    }


}
