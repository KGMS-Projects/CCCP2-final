package usecases;

import com.syos.entities.User;
import com.syos.usecases.RegisterUserUseCase;
import com.syos.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("Register User Use Case Tests")
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        registerUserUseCase = new RegisterUserUseCase(userRepository);
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("Should register user successfully with valid data")
    void shouldRegisterUserSuccessfully() throws RegisterUserUseCase.RegistrationException {
        // ARRANGE - Tell mock that email doesn't exist
        when(userRepository.existsByEmail("john@email.com")).thenReturn(false);

        // ACT
        User result = registerUserUseCase.execute("John Doe", "john@email.com", "password123", "123 Main St");

        // ASSERT
        assertNotNull(result);
        assertNotNull(result.getUserId()); // UUID should be generated
        assertEquals("John Doe", result.getName());
        assertEquals("john@email.com", result.getEmail());
        assertEquals("123 Main St", result.getAddress());

        // VERIFY - Check that save was called
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should generate unique user ID")
    void shouldGenerateUniqueUserId() throws RegisterUserUseCase.RegistrationException {
        // ARRANGE
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // ACT
        User user1 = registerUserUseCase.execute("User 1", "user1@email.com", "pass1234", "Addr 1");
        User user2 = registerUserUseCase.execute("User 2", "user2@email.com", "pass1234", "Addr 2");

        // ASSERT - IDs should be different
        assertNotEquals(user1.getUserId(), user2.getUserId());
    }

    @Test
    @DisplayName("Should hash password before saving")
    void shouldHashPassword() throws RegisterUserUseCase.RegistrationException {
        // ARRANGE
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);

        // ACT
        User result = registerUserUseCase.execute("Test User", "test@email.com", "password123", "Address");

        // ASSERT - Password hash should not equal plain password
        assertNotEquals("password123", result.getPasswordHash());
    }

    // ==================== VALIDATION ERROR TESTS ====================

    @Test
    @DisplayName("Should throw exception for null name")
    void shouldThrowExceptionForNullName() {
        // ACT & ASSERT
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute(null, "test@email.com", "password", "Address"));
        assertEquals("Name cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for empty name")
    void shouldThrowExceptionForEmptyName() {
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute("   ", "test@email.com", "password", "Address"));
        assertEquals("Name cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null email")
    void shouldThrowExceptionForNullEmail() {
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute("John", null, "password", "Address"));
        assertEquals("Invalid email address", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for invalid email without @")
    void shouldThrowExceptionForInvalidEmail() {
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute("John", "invalidemail", "password", "Address"));
        assertEquals("Invalid email address", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for null password")
    void shouldThrowExceptionForNullPassword() {
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute("John", "john@email.com", null, "Address"));
        assertEquals("Password must be at least 4 characters", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for short password")
    void shouldThrowExceptionForShortPassword() {
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute("John", "john@email.com", "abc", "Address"));
        assertEquals("Password must be at least 4 characters", exception.getMessage());
    }

    // ==================== DUPLICATE EMAIL TEST ====================

    @Test
    @DisplayName("Should throw exception for duplicate email")
    void shouldThrowExceptionForDuplicateEmail() {
        // ARRANGE - Email already exists
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        // ACT & ASSERT
        RegisterUserUseCase.RegistrationException exception = assertThrows(
                RegisterUserUseCase.RegistrationException.class,
                () -> registerUserUseCase.execute("John", "existing@email.com", "password", "Address"));
        assertEquals("Email already registered", exception.getMessage());

        // VERIFY - save should NOT be called for duplicate emails
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== REPOSITORY INTERACTION TESTS ====================

    @Test
    @DisplayName("Should check email existence before registering")
    void shouldCheckEmailExistence() throws RegisterUserUseCase.RegistrationException {
        // ARRANGE
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);

        // ACT
        registerUserUseCase.execute("New User", "new@email.com", "password", "Address");

        // VERIFY
        verify(userRepository).existsByEmail("new@email.com");
    }

    @Test
    @DisplayName("Should call repository save with user")
    void shouldCallRepositorySave() throws RegisterUserUseCase.RegistrationException {
        // ARRANGE
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);

        // ACT
        registerUserUseCase.execute("Test", "test@email.com", "password", "Addr");

        // VERIFY - save was called exactly once
        verify(userRepository, times(1)).save(any(User.class));
    }
}
