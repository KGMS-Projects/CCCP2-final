package usecases.strategies;

import com.syos.entities.StockBatch;
import com.syos.usecases.strategies.FIFOStockSelectionStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@DisplayName("FIFO Stock Selection Strategy Tests")
class FIFOStockSelectionStrategyTest {

    private FIFOStockSelectionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FIFOStockSelectionStrategy();
    }

    // TEST 1: Should return null for null list
    @Test
    void shouldReturnNullForNullList() {
        assertNull(strategy.selectBatch(null));
    }

    // TEST 2: Should return null for empty list
    @Test
    void shouldReturnNullForEmptyList() {
        assertNull(strategy.selectBatch(new ArrayList<>()));
    }

    // TEST 3: Should select oldest batch (FIFO)
    @Test
    void shouldSelectOldestBatch() {
        StockBatch oldBatch = new StockBatch("P001",
                LocalDate.now().minusDays(10),  // Purchased 10 days ago (OLDEST)
                50,
                LocalDate.now().plusDays(30));

        StockBatch newBatch = new StockBatch("P001",
                LocalDate.now().minusDays(2),   // Purchased 2 days ago
                50,
                LocalDate.now().plusDays(30));

        StockBatch result = strategy.selectBatch(Arrays.asList(newBatch, oldBatch));

        assertEquals(oldBatch, result); // Should select oldest
    }

    // TEST 4: Should skip batches with zero quantity
    @Test
    void shouldSkipBatchesWithZeroQuantity() {
        StockBatch emptyBatch = new StockBatch("P001",
                LocalDate.now().minusDays(10),  // Oldest but empty
                0,                               // ZERO quantity
                LocalDate.now().plusDays(30));

        StockBatch availableBatch = new StockBatch("P001",
                LocalDate.now().minusDays(2),
                50,                              // Has stock
                LocalDate.now().plusDays(30));

        StockBatch result = strategy.selectBatch(Arrays.asList(emptyBatch, availableBatch));

        assertEquals(availableBatch, result); // Should skip empty batch
    }

    // TEST 5: Should skip expired batches
    @Test
    void shouldSkipExpiredBatches() {
        StockBatch expiredBatch = new StockBatch("P001",
                LocalDate.now().minusDays(10),
                50,
                LocalDate.now().minusDays(1));  // EXPIRED yesterday

        StockBatch validBatch = new StockBatch("P001",
                LocalDate.now().minusDays(2),
                50,
                LocalDate.now().plusDays(30));  // Valid expiry

        StockBatch result = strategy.selectBatch(Arrays.asList(expiredBatch, validBatch));

        assertEquals(validBatch, result); // Should skip expired
    }

    // TEST 6: Should return null if all batches expired
    @Test
    void shouldReturnNullIfAllBatchesExpired() {
        StockBatch expired1 = new StockBatch("P001",
                LocalDate.now().minusDays(10), 50, LocalDate.now().minusDays(5));
        StockBatch expired2 = new StockBatch("P001",
                LocalDate.now().minusDays(5), 50, LocalDate.now().minusDays(1));

        assertNull(strategy.selectBatch(Arrays.asList(expired1, expired2)));
    }

    // TEST 7: Should return single batch if only one available
    @Test
    void shouldReturnSingleBatchIfOnlyOne() {
        StockBatch singleBatch = new StockBatch("P001",
                LocalDate.now(), 100, LocalDate.now().plusDays(30));

        assertEquals(singleBatch, strategy.selectBatch(Arrays.asList(singleBatch)));
    }
}