package com.example.mdreader.service;

import com.example.mdreader.model.ReaderPreferences;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PreferenceService {
    private static final int MAX_RECENT_FILES = 10;

    private final ObjectMapper objectMapper;
    private final Path preferencePath;

    public PreferenceService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.preferencePath = Path.of(System.getProperty("user.home"),
                "Library", "Application Support", "md-reader", "preferences.json");
    }

    public ReaderPreferences load() {
        if (!Files.exists(preferencePath)) {
            return ReaderPreferences.defaults();
        }

        try {
            return objectMapper.readValue(preferencePath.toFile(), ReaderPreferences.class);
        } catch (IOException ex) {
            return ReaderPreferences.defaults();
        }
    }

    public void save(ReaderPreferences preferences) {
        try {
            Files.createDirectories(preferencePath.getParent());
            objectMapper.writeValue(preferencePath.toFile(), preferences);
        } catch (IOException ignored) {
            // First milestone keeps persistence errors non-fatal.
        }
    }

    public void rememberFile(ReaderPreferences preferences, Path file) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(file.toAbsolutePath().normalize().toString());
        ordered.addAll(preferences.recentFiles());

        List<String> recentFiles = new ArrayList<>(ordered);
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles = recentFiles.subList(0, MAX_RECENT_FILES);
        }
        preferences.recentFiles().clear();
        preferences.recentFiles().addAll(recentFiles);
    }
}
