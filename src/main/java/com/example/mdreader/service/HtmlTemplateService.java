package com.example.mdreader.service;

import com.example.mdreader.model.ReaderPreferences;
import com.example.mdreader.model.RenderedDocument;
import com.example.mdreader.model.Theme;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class HtmlTemplateService {

    public String buildHtml(RenderedDocument document, ReaderPreferences preferences) {
        String themeClass = preferences.theme() == Theme.DARK ? "theme-dark" : "theme-light";
        String title = escapeHtml(document.title());
        String body = Objects.requireNonNullElse(document.htmlBody(), "");
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                  <style>%s</style>
                  <script>%s</script>
                </head>
                <body class="%s" style="--reader-font-size: %dpx;">
                  <main class="markdown-body">%s</main>
                </body>
                </html>
                """.formatted(
                title,
                readResource("css/base.css") + "\n" + readResource(themeResource(preferences.theme())),
                readResource("js/reader.js"),
                themeClass,
                preferences.fontSize(),
                body
        );
    }

    public String buildWelcomePage(ReaderPreferences preferences) {
        RenderedDocument document = new RenderedDocument(
                "欢迎使用 md-reader",
                """
                <h1 id="welcome">欢迎使用 md-reader</h1>
                <p>这是一个基于 JavaFX 的轻量 Markdown 阅读器项目骨架。</p>
                <ul>
                  <li>点击上方“打开”选择本地 Markdown 文件</li>
                  <li>左侧目录会在加载文档后自动生成</li>
                  <li>支持主题切换和字号调整</li>
                </ul>
                """,
                java.util.List.of(),
                null,
                null
        );
        return buildHtml(document, preferences);
    }

    public String buildErrorPage(String message, ReaderPreferences preferences) {
        RenderedDocument document = new RenderedDocument(
                "加载失败",
                "<h1>加载失败</h1><p>" + escapeHtml(message) + "</p>",
                java.util.List.of(),
                null,
                null
        );
        return buildHtml(document, preferences);
    }

    private String themeResource(Theme theme) {
        return theme == Theme.DARK ? "css/dark.css" : "css/light.css";
    }

    private String readResource(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("缺少资源文件: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("读取资源失败: " + resourcePath, ex);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
