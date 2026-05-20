package com.syos.entities;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a user in the online store.
 * Follows Single Responsibility Principle.
 */
public class User {
    private final String userId;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String address;
    private final LocalDateTime registrationDate;

    public User(String userId, String name, String email, String passwordHash, String address,
            LocalDateTime registrationDate) {
        validateUser(userId, name, email, passwordHash);
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.address = address;
        this.registrationDate = registrationDate;
    }

    private void validateUser(String userId, String name, String email, String passwordHash) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAddress() {
        return address;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
