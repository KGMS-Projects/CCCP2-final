package entities;

import com.syos.entities.Bill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@DisplayName("Bill Entity Tests")
class BillTest {

    // ==================== HELPER METHODS ====================

    private Bill.BillItem createTestItem(String code, String name, int quantity, double price, double discount) {
        return new Bill.BillItem(code, name, "pcs", quantity, price, discount);
    }

    private Bill.BillItem createSimpleItem(double price, int quantity) {
        return new Bill.BillItem("P001", "Test Product", "pcs", quantity, price, 0);
    }

    // ==================== BUILDER TESTS ====================

    @Test
    @DisplayName("Should create bill with builder pattern")
    void shouldCreateBillWithBuilder() {
        Bill.BillItem item = createSimpleItem(10.00, 2);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .transactionType(Bill.TransactionType.COUNTER)
                .build();

        assertNotNull(bill);
        assertEquals(1, bill.getSerialNumber());
        assertEquals(Bill.TransactionType.COUNTER, bill.getTransactionType());
    }

    @Test
    @DisplayName("Should set bill date to now by default")
    void shouldSetBillDateToNowByDefault() {
        Bill.BillItem item = createSimpleItem(10.00, 1);
        LocalDateTime before = LocalDateTime.now();

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .build();

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(bill.getBillDate());
        assertTrue(!bill.getBillDate().isBefore(before) && !bill.getBillDate().isAfter(after));
    }

    @Test
    @DisplayName("Should set custom bill date")
    void shouldSetCustomBillDate() {
        Bill.BillItem item = createSimpleItem(10.00, 1);
        LocalDateTime customDate = LocalDateTime.of(2025, 1, 15, 10, 30);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .billDate(customDate)
                .build();

        assertEquals(customDate, bill.getBillDate());
    }

    @Test
    @DisplayName("Should add items using addItem method")
    void shouldAddItemsUsingAddItem() {
        Bill.BillItem item1 = createTestItem("P001", "Product 1", 1, 10.00, 0);
        Bill.BillItem item2 = createTestItem("P002", "Product 2", 2, 20.00, 0);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item1)
                .addItem(item2)
                .cashTendered(100.00)
                .build();

        assertEquals(2, bill.getItems().size());
    }

    @Test
    @DisplayName("Should add items using items list method")
    void shouldAddItemsUsingItemsList() {
        List<Bill.BillItem> items = Arrays.asList(
                createTestItem("P001", "Product 1", 1, 10.00, 0),
                createTestItem("P002", "Product 2", 2, 20.00, 0));

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .items(items)
                .cashTendered(100.00)
                .build();

        assertEquals(2, bill.getItems().size());
    }

    @Test
    @DisplayName("Should default transaction type to COUNTER")
    void shouldDefaultTransactionTypeToCounter() {
        Bill.BillItem item = createSimpleItem(10.00, 1);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .build();

        assertEquals(Bill.TransactionType.COUNTER, bill.getTransactionType());
    }

    @Test
    @DisplayName("Should set online transaction type")
    void shouldSetOnlineTransactionType() {
        Bill.BillItem item = createSimpleItem(10.00, 1);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .transactionType(Bill.TransactionType.ONLINE)
                .customerId("CUST001")
                .build();

        assertEquals(Bill.TransactionType.ONLINE, bill.getTransactionType());
        assertEquals("CUST001", bill.getCustomerId());
    }

    // ==================== CALCULATION TESTS ====================

    @Test
    @DisplayName("Should calculate subtotal correctly")
    void shouldCalculateSubtotalCorrectly() {
        Bill.BillItem item1 = createSimpleItem(10.00, 2); // 20.00
        Bill.BillItem item2 = createSimpleItem(15.00, 3); // 45.00

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item1)
                .addItem(item2)
                .cashTendered(100.00)
                .build();

        assertEquals(65.00, bill.getSubtotal(), 0.01);
    }

    @Test
    @DisplayName("Should calculate discount correctly")
    void shouldCalculateDiscountCorrectly() {
        // Item: 10.00 * 2 = 20.00, 10% discount = 2.00
        Bill.BillItem item = createTestItem("P001", "Product", 2, 10.00, 10.0);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .build();

        assertEquals(2.00, bill.getDiscount(), 0.01);
    }

    @Test
    @DisplayName("Should calculate total correctly with discount")
    void shouldCalculateTotalCorrectlyWithDiscount() {
        // Item: 10.00 * 2 = 20.00, 10% discount = 2.00, total = 18.00
        Bill.BillItem item = createTestItem("P001", "Product", 2, 10.00, 10.0);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .build();

        assertEquals(18.00, bill.getTotal(), 0.01);
    }

    @Test
    @DisplayName("Should calculate change correctly")
    void shouldCalculateChangeCorrectly() {
        Bill.BillItem item = createSimpleItem(10.00, 2); // Total: 20.00

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(50.00)
                .build();

        assertEquals(30.00, bill.getChange(), 0.01);
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should throw exception for empty items")
    void shouldThrowExceptionForEmptyItems() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Bill.Builder()
                        .serialNumber(1)
                        .cashTendered(100.00)
                        .build());

        assertEquals("Bill must have at least one item", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for insufficient cash tendered")
    void shouldThrowExceptionForInsufficientCashTendered() {
        Bill.BillItem item = createSimpleItem(50.00, 2); // Total: 100.00

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Bill.Builder()
                        .serialNumber(1)
                        .addItem(item)
                        .cashTendered(50.00) // Less than 100.00
                        .build());

        assertEquals("Cash tendered must be greater than or equal to total", exception.getMessage());
    }

    // ==================== IMMUTABILITY TESTS ====================

    @Test
    @DisplayName("Should return unmodifiable items list")
    void shouldReturnUnmodifiableItemsList() {
        Bill.BillItem item = createSimpleItem(10.00, 1);

        Bill bill = new Bill.Builder()
                .serialNumber(1)
                .addItem(item)
                .cashTendered(100.00)
                .build();

        List<Bill.BillItem> items = bill.getItems();

        assertThrows(UnsupportedOperationException.class, () -> items.add(createSimpleItem(5.00, 1)));
    }

    // ==================== BILL ITEM TESTS ====================

    @Test
    @DisplayName("Should create bill item correctly")
    void shouldCreateBillItemCorrectly() {
        Bill.BillItem item = new Bill.BillItem("P001", "Test Product", "kg", 5, 10.00, 15.0);

        assertEquals("P001", item.getProductCode());
        assertEquals("Test Product", item.getProductName());
        assertEquals("kg", item.getUnit());
        assertEquals(5, item.getQuantity());
        assertEquals(10.00, item.getPrice(), 0.01);
        assertEquals(15.0, item.getDiscountPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should calculate item total correctly")
    void shouldCalculateItemTotalCorrectly() {
        Bill.BillItem item = new Bill.BillItem("P001", "Product", "pcs", 3, 25.00, 0);

        assertEquals(75.00, item.getItemTotal(), 0.01);
    }

    @Test
    @DisplayName("Should calculate item discount amount correctly")
    void shouldCalculateItemDiscountAmountCorrectly() {
        // 3 * 25.00 = 75.00, 20% discount = 15.00
        Bill.BillItem item = new Bill.BillItem("P001", "Product", "pcs", 3, 25.00, 20.0);

        assertEquals(15.00, item.getDiscountAmount(), 0.01);
    }

    @Test
    @DisplayName("Should calculate item final price correctly")
    void shouldCalculateItemFinalPriceCorrectly() {
        // 3 * 25.00 = 75.00, 20% discount = 15.00, final = 60.00
        Bill.BillItem item = new Bill.BillItem("P001", "Product", "pcs", 3, 25.00, 20.0);

        assertEquals(60.00, item.getFinalPrice(), 0.01);
    }

    // ==================== TRANSACTION TYPE TESTS ====================

    @Test
    @DisplayName("Should have COUNTER transaction type")
    void shouldHaveCounterTransactionType() {
        assertEquals("COUNTER", Bill.TransactionType.COUNTER.name());
    }

    @Test
    @DisplayName("Should have ONLINE transaction type")
    void shouldHaveOnlineTransactionType() {
        assertEquals("ONLINE", Bill.TransactionType.ONLINE.name());
    }

    // ==================== toString TESTS ====================

    @Test
    @DisplayName("Should generate toString correctly")
    void shouldGenerateToStringCorrectly() {
        Bill.BillItem item = createSimpleItem(10.00, 1);

        Bill bill = new Bill.Builder()
                .serialNumber(123)
                .addItem(item)
                .cashTendered(100.00)
                .build();

        String billString = bill.toString();
        assertTrue(billString.contains("serialNumber=123"));
        assertTrue(billString.contains("transactionType=COUNTER"));
    }

    @Test
    @DisplayName("Should generate BillItem toString correctly")
    void shouldGenerateBillItemToStringCorrectly() {
        Bill.BillItem item = new Bill.BillItem("P001", "Test Product", "pcs", 5, 10.00, 10.0);

        String itemString = item.toString();
        assertTrue(itemString.contains("productName='Test Product'"));
        assertTrue(itemString.contains("quantity=5"));
        assertTrue(itemString.contains("price=10.0"));
    }
}
