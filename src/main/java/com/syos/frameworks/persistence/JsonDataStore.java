package com.syos.frameworks.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class for managing JSON data storage.
 * Implements Singleton Pattern - ensures single instance for data management.
 * Provides centralized data persistence.
 */
public class JsonDataStore {
    private static JsonDataStore instance;
    private final Gson gson;
    private final String dataDirectory;

    private JsonDataStore() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();
        this.dataDirectory = "data";
        initializeDataDirectory();
    }

    /**
     * Gets the singleton instance.
     * Thread-safe lazy initialization.
     */
    public static synchronized JsonDataStore getInstance() {
        if (instance == null) {
            instance = new JsonDataStore();
        }
        return instance;
    }

    private void initializeDataDirectory() {
        try {
            Files.createDirectories(Paths.get(dataDirectory));
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    public <T> void save(String filename, List<T> data) {
        String filepath = dataDirectory + File.separator + filename;
        try (Writer writer = new FileWriter(filepath)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Failed to save data to " + filename + ": " + e.getMessage());
        }
    }

    public <T> List<T> load(String filename, Type type) {
        String filepath = dataDirectory + File.separator + filename;
        File file = new File(filepath);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(filepath)) {
            List<T> data = gson.fromJson(reader, type);
            return data != null ? data : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Failed to load data from " + filename + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Gson getGson() {
        return gson;
    }
}
