package com.example.mdreader.service;

import com.example.mdreader.model.ReaderPreferences;
import com.example.mdreader.model.RenderedDocument;
import com.example.mdreader.model.Theme;
import com.example.mdreader.model.TocItem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class HtmlTemplateService {

    public String buildHtml(RenderedDocument document, ReaderPreferences preferences) {
        String themeClass = preferences.theme() == Theme.DARK ? "theme-dark" : "theme-light";
        String title = escapeHtml(document.title());
        String body = Objects.requireNonNullElse(document.htmlBody(), "");
        String tocHtml = buildTocHtml(document.tocItems());
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
                  <div class="reader-shell">
                    <aside class="reader-sidebar">
                      <div class="sidebar-title">目录</div>
                      <nav class="toc-nav">%s</nav>
                    </aside>
                    <main class="markdown-body">%s</main>
                  </div>
                </body>
                </html>
                """.formatted(
                title,
                readResource("css/base.css") + "\n" + readResource(themeResource(preferences.theme())),
                readResource("js/reader.js"),
                themeClass,
                preferences.fontSize(),
                tocHtml,
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
                  <li>页面左侧会在加载文档后自动生成目录</li>
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

    private String buildTocHtml(List<TocItem> tocItems) {
        if (tocItems == null || tocItems.isEmpty()) {
            return "<div class=\"toc-empty\">当前文档没有目录项</div>";
        }

        StringBuilder builder = new StringBuilder();
        appendTocList(builder, tocItems);
        return builder.toString();
    }

    private void appendTocList(StringBuilder builder, List<TocItem> items) {
        builder.append("<ul class=\"toc-list\">");
        for (TocItem item : items) {
            builder.append("<li class=\"toc-item toc-level-")
                    .append(item.level())
                    .append("\">")
                    .append("<a class=\"toc-link\" href=\"#")
                    .append(escapeHtml(item.anchorId()))
                    .append("\" data-anchor=\"")
                    .append(escapeHtml(item.anchorId()))
                    .append("\">")
                    .append(escapeHtml(item.text()))
                    .append("</a>");
            if (item.children() != null && !item.children().isEmpty()) {
                appendTocList(builder, item.children());
            }
            builder.append("</li>");
        }
        builder.append("</ul>");
    }

}
