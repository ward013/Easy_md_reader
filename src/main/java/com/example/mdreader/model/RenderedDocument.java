package com.example.mdreader.model;

import java.nio.file.Path;
import java.util.List;

public record RenderedDocument(
        String title,
        String htmlBody,
        List<TocItem> tocItems,
        Path sourcePath,
        Path baseDir
) {
}
