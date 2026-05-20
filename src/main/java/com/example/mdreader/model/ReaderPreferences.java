package com.example.mdreader.model;

import java.util.ArrayList;
import java.util.List;

public record ReaderPreferences(
        Theme theme,
        int fontSize,
        double windowWidth,
        double windowHeight,
        List<String> recentFiles
) {
    public static ReaderPreferences defaults() {
        return new ReaderPreferences(Theme.LIGHT, 16, 1280, 820, new ArrayList<>());
    }

    public ReaderPreferences withTheme(Theme nextTheme) {
        return new ReaderPreferences(nextTheme, fontSize, windowWidth, windowHeight, new ArrayList<>(recentFiles));
    }

    public ReaderPreferences withFontSize(int nextFontSize) {
        return new ReaderPreferences(theme, nextFontSize, windowWidth, windowHeight, new ArrayList<>(recentFiles));
    }

    public ReaderPreferences withRecentFiles(List<String> nextRecentFiles) {
        return new ReaderPreferences(theme, fontSize, windowWidth, windowHeight, new ArrayList<>(nextRecentFiles));
    }
}
