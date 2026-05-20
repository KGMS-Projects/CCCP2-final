package com.syos.usecases.repositories;

import com.syos.entities.User;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User data access.
 */
public interface UserRepository {
    void save(User user);

    Optional<User> findById(String userId);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    boolean existsByEmail(String email);
}
