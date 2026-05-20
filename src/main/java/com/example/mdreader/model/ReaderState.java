package com.example.mdreader.model;

import java.nio.file.Path;

public class ReaderState {
    private Path currentFile;
    private RenderedDocument currentDocument;
    private String currentSearchKeyword;

    public Path getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(Path currentFile) {
        this.currentFile = currentFile;
    }

    public RenderedDocument getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(RenderedDocument currentDocument) {
        this.currentDocument = currentDocument;
    }

    public String getCurrentSearchKeyword() {
        return currentSearchKeyword;
    }

    public void setCurrentSearchKeyword(String currentSearchKeyword) {
        this.currentSearchKeyword = currentSearchKeyword;
    }
}
