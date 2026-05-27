package com.example.mdreader.service;

import javafx.scene.web.WebEngine;

public class SearchService {

    public boolean find(WebEngine engine, String keyword) {
        String escaped = escapeJs(keyword);
        Object result = engine.executeScript("""
                clearSearchHighlights();
                window.find('%s');
                """.formatted(escaped));
        return result instanceof Boolean booleanResult && booleanResult;
    }

    public void findNext(WebEngine engine) {
        engine.executeScript("window.find('', false, false, true, false, true, false);");
    }

    public void findPrevious(WebEngine engine) {
        engine.executeScript("window.find('', false, true, true, false, true, false);");
    }

    public boolean jumpToAnchor(WebEngine engine, String anchorId) {
        Object result = engine.executeScript("""
                (function() {
                    return jumpToAnchor('%s');
                })();
                """.formatted(escapeJs(anchorId)));
        return result instanceof Boolean booleanResult && booleanResult;
    }

    private String escapeJs(String raw) {
        return raw.replace("\\", "\\\\").replace("'", "\\'");
    }
}
