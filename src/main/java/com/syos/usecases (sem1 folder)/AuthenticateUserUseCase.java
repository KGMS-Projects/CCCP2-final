package com.syos.usecases;

import com.syos.entities.User;
import com.syos.usecases.repositories.UserRepository;

/**
 * Use case for user authentication.
 */
public class AuthenticateUserUseCase {
    private final UserRepository userRepository;

    public AuthenticateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User execute(String email, String password) throws AuthenticationException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        String passwordHash = hashPassword(password);
        if (!user.getPasswordHash().equals(passwordHash)) {
            throw new AuthenticationException("Invalid email or password");
        }

        return user;
    }

    private String hashPassword(String password) {
        // Simple hash for demonstration - matches RegisterUserUseCase
        return Integer.toString(password.hashCode());
    }

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
