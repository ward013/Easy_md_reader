package com.example.mdreader.model;

import java.util.List;

public record TocItem(
        String text,
        String anchorId,
        int level,
        List<TocItem> children
) {
    @Override
    public String toString() {
        return text;
    }
}
