package entities;

import com.syos.entities.StockBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

@DisplayName("StockBatch Entity Tests")
class StockBatchTest {

    //Test Basic Creation
    @Test
    @DisplayName("Should create stock batch with correct values")
    void shouldCreateStockBatchWithCorrectValues() {
        LocalDate purchaseDate = LocalDate.now();
        LocalDate expiryDate = LocalDate.now().plusMonths(6);

        StockBatch batch = new StockBatch("P001", purchaseDate, 100, expiryDate);

        assertEquals("P001", batch.getProductCode());
        assertEquals(purchaseDate, batch.getPurchaseDate());
        assertEquals(100, batch.getQuantity());
        assertEquals(expiryDate, batch.getExpiryDate());
        assertNotNull(batch.getBatchId());
    }

    @Test
    @DisplayName("Should create stock batch with existing batch ID")
    void shouldCreateStockBatchWithExistingBatchId() {
        StockBatch batch = new StockBatch("B100", "P001", LocalDate.now(), 50, LocalDate.now().plusMonths(6));

        assertEquals("B100", batch.getBatchId());
    }

    //Test Validation Errors
    @Test
    @DisplayName("Should throw exception for empty product code")
    void shouldThrowExceptionForEmptyProductCode() {
        assertThrows(IllegalArgumentException.class, () ->
                new StockBatch("", LocalDate.now(), 100, LocalDate.now().plusMonths(6)));
    }

    @Test
    @DisplayName("Should throw exception for zero quantity")
    void shouldThrowExceptionForZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                new StockBatch("P001", LocalDate.now(), 0, LocalDate.now().plusMonths(6)));
    }

    @Test
    @DisplayName("Should throw exception when expiry before purchase")
    void shouldThrowExceptionWhenExpiryBeforePurchase() {
        assertThrows(IllegalArgumentException.class, () ->
                new StockBatch("P001", LocalDate.now(), 100, LocalDate.now().minusDays(1)));
    }

    //Test Reduce Quantity
    @Test
    @DisplayName("Should reduce quantity correctly")
    void shouldReduceQuantityCorrectly() {
        StockBatch batch = new StockBatch("P001", LocalDate.now(), 100, LocalDate.now().plusMonths(6));
        batch.reduceQuantity(30);
        assertEquals(70, batch.getQuantity());
    }

    @Test
    @DisplayName("Should throw exception when reducing more than available")
    void shouldThrowExceptionWhenReducingMoreThanAvailable() {
        StockBatch batch = new StockBatch("P001", LocalDate.now(), 50, LocalDate.now().plusMonths(6));
        assertThrows(IllegalArgumentException.class, () -> batch.reduceQuantity(60));
    }


    //Test Expiry Methods
    @Test
    @DisplayName("Should detect expired batch")
    void shouldDetectExpiredBatch() {
        StockBatch batch = new StockBatch("P001", LocalDate.now().minusMonths(7), 100, LocalDate.now().minusDays(1));
        assertTrue(batch.isExpired());
    }

    @Test
    @DisplayName("Should detect non-expired batch")
    void shouldDetectNonExpiredBatch() {
        StockBatch batch = new StockBatch("P001", LocalDate.now(), 100, LocalDate.now().plusMonths(6));
        assertFalse(batch.isExpired());
    }

    @Test
    @DisplayName("Should calculate days until expiry")
    void shouldCalculateDaysUntilExpiry() {
        StockBatch batch = new StockBatch("P001", LocalDate.now(), 100, LocalDate.now().plusDays(30));
        assertEquals(30, batch.getDaysUntilExpiry());
    }
}