package com.syos.frameworks.persistence;

import com.google.gson.reflect.TypeToken;
import com.syos.entities.User;
import com.syos.usecases.repositories.UserRepository;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-based implementation of UserRepository.
 */
public class JsonUserRepository implements UserRepository {
    private static final String FILENAME = "users.json";
    private final JsonDataStore dataStore;
    private final Type listType = new TypeToken<ArrayList<UserData>>() {
    }.getType();

    public JsonUserRepository() {
        this.dataStore = JsonDataStore.getInstance();
    }

    @Override
    public void save(User user) {
        List<UserData> users = loadAll();
        users.add(toData(user));
        dataStore.save(FILENAME, users);
    }

    @Override
    public Optional<User> findById(String userId) {
        return loadAll().stream()
                .filter(u -> u.userId.equals(userId))
                .map(this::toEntity)
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return loadAll().stream()
                .filter(u -> u.email.equals(email))
                .map(this::toEntity)
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return loadAll().stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return loadAll().stream()
                .anyMatch(u -> u.email.equals(email));
    }

    private List<UserData> loadAll() {
        return dataStore.load(FILENAME, listType);
    }

    private UserData toData(User user) {
        UserData data = new UserData();
        data.userId = user.getUserId();
        data.name = user.getName();
        data.email = user.getEmail();
        data.passwordHash = user.getPasswordHash();
        data.address = user.getAddress();
        data.registrationDate = user.getRegistrationDate().toString();
        return data;
    }

    private User toEntity(UserData data) {
        return new User(
                data.userId,
                data.name,
                data.email,
                data.passwordHash,
                data.address,
                LocalDateTime.parse(data.registrationDate));
    }

    private static class UserData {
        String userId;
        String name;
        String email;
        String passwordHash;
        String address;
        String registrationDate;
    }
}
