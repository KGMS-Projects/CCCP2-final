package entities;


import com.syos.entities.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Entity Tests")
class ProductTest {

    @Test
    @DisplayName("Should create product with valid data")
    void shouldCreateProductWithValidData() {
        Product product = new Product.Builder()
                .code("P001")
                .name("Rice")
                .unit("kg")
                .price(150.00)
                .discountPercentage(10.0)
                .build();

        assertEquals("P001", product.getCode());
        assertEquals("Rice", product.getName());
        assertEquals("kg", product.getUnit());
        assertEquals(150.00, product.getPrice());
        assertEquals(10.0, product.getDiscountPercentage());
    }

    @Test
    @DisplayName("Should calculate discounted price correctly")
    void shouldCalculateDiscountedPrice() {
        Product product = new Product.Builder()
                .code("P001")
                .name("Rice")
                .unit("kg")
                .price(100.00)
                .discountPercentage(20.0)
                .build();

        assertEquals(80.00, product.getDiscountedPrice(), 0.01);
    }

    @Test
    @DisplayName("Should return original price when no discount")
    void shouldReturnOriginalPriceWhenNoDiscount() {
        Product product = new Product.Builder()
                .code("P001")
                .name("Rice")
                .unit("kg")
                .price(100.00)
                .discountPercentage(0.0)
                .build();

        assertEquals(100.00, product.getDiscountedPrice(), 0.01);
    }

    @Test
    @DisplayName("Should throw exception for empty product code")
    void shouldThrowExceptionForEmptyProductCode() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Product.Builder()
                    .code("")
                    .name("Rice")
                    .unit("kg")
                    .price(100.00)
                    .build();
        });
    }

    @Test
    @DisplayName("Should throw exception for null product name")
    void shouldThrowExceptionForNullProductName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Product.Builder()
                    .code("P001")
                    .name(null)
                    .unit("kg")
                    .price(100.00)
                    .build();
        });
    }

    @Test
    @DisplayName("Should throw exception for negative price")
    void shouldThrowExceptionForNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Product.Builder()
                    .code("P001")
                    .name("Rice")
                    .unit("kg")
                    .price(-100.00)
                    .build();
        });
    }

    @Test
    @DisplayName("Should throw exception for discount over 100%")
    void shouldThrowExceptionForDiscountOver100() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Product.Builder()
                    .code("P001")
                    .name("Rice")
                    .unit("kg")
                    .price(100.00)
                    .discountPercentage(150.0)
                    .build();
        });
    }

    @Test
    @DisplayName("Products with same code should be equal")
    void productsWithSameCodeShouldBeEqual() {
        Product product1 = new Product.Builder().code("P001").name("Rice").unit("kg").price(100.00).build();
        Product product2 = new Product.Builder().code("P001").name("Sugar").unit("kg").price(200.00).build();

        assertEquals(product1, product2);
        assertEquals(product1.hashCode(), product2.hashCode());
    }

    @Test
    @DisplayName("Products with different codes should not be equal")
    void productsWithDifferentCodesShouldNotBeEqual() {
        Product product1 = new Product.Builder().code("P001").name("Rice").unit("kg").price(100.00).build();
        Product product2 = new Product.Builder().code("P002").name("Rice").unit("kg").price(100.00).build();

        assertNotEquals(product1, product2);
    }
}
