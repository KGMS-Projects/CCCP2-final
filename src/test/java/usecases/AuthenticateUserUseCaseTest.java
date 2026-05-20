package usecases;

import com.syos.entities.User;
import com.syos.usecases.AuthenticateUserUseCase;
import com.syos.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authenticate User Use Case Tests")
class AuthenticateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private AuthenticateUserUseCase authenticateUserUseCase;

    @BeforeEach
    void setUp() {
        authenticateUserUseCase = new AuthenticateUserUseCase(userRepository);
    }

    @Test
    @DisplayName("Should authenticate user with correct credentials")
    void shouldAuthenticateWithCorrectCredentials() throws AuthenticateUserUseCase.AuthenticationException {
        String passwordHash = Integer.toString("password123".hashCode());
        User user = new User("U001", "John Doe", "john@email.com", passwordHash, "Address", LocalDateTime.now());
        when(userRepository.findByEmail("john@email.com")).thenReturn(Optional.of(user));

        User result = authenticateUserUseCase.execute("john@email.com", "password123");

        assertNotNull(result);
        assertEquals("U001", result.getUserId());
    }

    @Test
    @DisplayName("Should throw exception for wrong password")
    void shouldThrowExceptionForWrongPassword() {
        String passwordHash = Integer.toString("correctPassword".hashCode());
        User user = new User("U001", "John", "john@email.com", passwordHash, "Addr", LocalDateTime.now());
        when(userRepository.findByEmail("john@email.com")).thenReturn(Optional.of(user));

        assertThrows(AuthenticateUserUseCase.AuthenticationException.class, () ->
                authenticateUserUseCase.execute("john@email.com", "wrongPassword"));
    }

    @Test
    @DisplayName("Should throw exception for non-existent email")
    void shouldThrowExceptionForNonExistentEmail() {
        when(userRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticateUserUseCase.AuthenticationException.class, () ->
                authenticateUserUseCase.execute("unknown@email.com", "password"));
    }

    @Test
    @DisplayName("Should call repository to find user")
    void shouldCallRepositoryToFindUser() throws AuthenticateUserUseCase.AuthenticationException {
        String passwordHash = Integer.toString("pass".hashCode());
        User user = new User("U001", "John", "test@email.com", passwordHash, "Addr", LocalDateTime.now());
        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));

        authenticateUserUseCase.execute("test@email.com", "pass");

        verify(userRepository).findByEmail("test@email.com");
    }
}