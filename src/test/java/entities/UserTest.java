package entities;
import com.syos.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("User Entity Tests")
class UserTest {

    //Test Basic Creation
    @Test
    @DisplayName("Should create user with correct values")
    void shouldCreateUserWithCorrectValues() {
        LocalDateTime registrationDate = LocalDateTime.now();

        User user = new User("U001", "John Doe", "john@email.com",
                "hashedPassword123", "123 Main St", registrationDate);

        assertEquals("U001", user.getUserId());
        assertEquals("John Doe", user.getName());
        assertEquals("john@email.com", user.getEmail());
        assertEquals("hashedPassword123", user.getPasswordHash());
        assertEquals("123 Main St", user.getAddress());
        assertEquals(registrationDate, user.getRegistrationDate());
    }


    //Test Validation Errors
    @Test
    @DisplayName("Should throw exception for empty user ID")
    void shouldThrowExceptionForEmptyUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("", "John Doe", "john@email.com", "hash123", "Address", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should throw exception for null user ID")
    void shouldThrowExceptionForNullUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, "John Doe", "john@email.com", "hash123", "Address", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should throw exception for empty name")
    void shouldThrowExceptionForEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("U001", "", "john@email.com", "hash123", "Address", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should throw exception for invalid email")
    void shouldThrowExceptionForInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("U001", "John Doe", "invalidemail", "hash123", "Address", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should throw exception for null email")
    void shouldThrowExceptionForNullEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("U001", "John Doe", null, "hash123", "Address", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should throw exception for empty password hash")
    void shouldThrowExceptionForEmptyPasswordHash() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("U001", "John Doe", "john@email.com", "", "Address", LocalDateTime.now()));
    }

    //Test Equality
    @Test
    @DisplayName("Should be equal if same user ID")
    void shouldBeEqualIfSameUserId() {
        User user1 = new User("U001", "John", "john@email.com", "hash1", "Addr1", LocalDateTime.now());
        User user2 = new User("U001", "Jane", "jane@email.com", "hash2", "Addr2", LocalDateTime.now());

        assertEquals(user1, user2);
    }

    @Test
    @DisplayName("Should not be equal if different user ID")
    void shouldNotBeEqualIfDifferentUserId() {
        User user1 = new User("U001", "John", "john@email.com", "hash1", "Addr1", LocalDateTime.now());
        User user2 = new User("U002", "John", "john@email.com", "hash1", "Addr1", LocalDateTime.now());

        assertNotEquals(user1, user2);
    }
}