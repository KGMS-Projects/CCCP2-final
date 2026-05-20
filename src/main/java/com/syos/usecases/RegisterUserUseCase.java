package com.syos.usecases;

import com.syos.entities.User;
import com.syos.usecases.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case for user registration.
 * Implements requirement: "The user needs to be registered with the system
 * first"
 */
public class RegisterUserUseCase {
    private final UserRepository userRepository;

    public RegisterUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String name, String email, String password, String address) throws RegistrationException {
        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            throw new RegistrationException("Name cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new RegistrationException("Invalid email address");
        }
        if (password == null || password.length() < 4) {
            throw new RegistrationException("Password must be at least 4 characters");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("Email already registered");
        }

        // Create user
        String userId = UUID.randomUUID().toString();
        String passwordHash = hashPassword(password);
        User user = new User(userId, name, email, passwordHash, address, LocalDateTime.now());

        // Save user
        userRepository.save(user);

        return user;
    }

    private String hashPassword(String password) {
        // Simple hash for demonstration - in production, use BCrypt or similar
        return Integer.toString(password.hashCode());
    }

    public static class RegistrationException extends Exception {
        public RegistrationException(String message) {
            super(message);
        }
    }
}
