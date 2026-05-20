package com.example.mdreader.model;

import java.nio.file.Path;

public record DocumentSource(
        Path path,
        String rawMarkdown,
        String fileName,
        Path baseDir
) {
}
