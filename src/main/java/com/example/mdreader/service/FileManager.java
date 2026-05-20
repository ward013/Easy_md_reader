package com.example.mdreader.service;

import com.example.mdreader.model.DocumentSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    public DocumentSource readMarkdown(Path path) throws IOException {
        Path normalizedPath = path.toAbsolutePath().normalize();
        String raw = Files.readString(normalizedPath, StandardCharsets.UTF_8);
        Path baseDir = normalizedPath.getParent() == null ? Path.of(".") : normalizedPath.getParent();
        return new DocumentSource(normalizedPath, raw, normalizedPath.getFileName().toString(), baseDir);
    }

    public boolean isMarkdownFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".markdown") || fileName.endsWith(".mdown");
    }
}
