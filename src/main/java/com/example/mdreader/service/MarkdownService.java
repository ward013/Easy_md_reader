package com.example.mdreader.service;

import com.example.mdreader.model.DocumentSource;
import com.example.mdreader.model.RenderedDocument;
import com.example.mdreader.model.TocItem;
import com.example.mdreader.util.ResourceResolver;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MarkdownService {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                AutolinkExtension.create(),
                TocExtension.create()
        ));

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    public RenderedDocument render(DocumentSource source) {
        Document document = parser.parse(source.rawMarkdown());
        String renderedHtml = renderer.render(document);
        ProcessedHtml processedHtml = processHeadings(renderedHtml);
        List<TocItem> tocItems = toTree(processedHtml.headings());
        String resolvedHtml = ResourceResolver.rewriteRelativeResources(processedHtml.htmlBody(), source.baseDir());
        String title = !tocItems.isEmpty() ? tocItems.getFirst().text() : source.fileName();

        return new RenderedDocument(title, resolvedHtml, tocItems, source.path(), source.baseDir());
    }

    private ProcessedHtml processHeadings(String html) {
        org.jsoup.nodes.Document document = Jsoup.parseBodyFragment(html);
        Elements headingElements = document.select("h1, h2, h3, h4, h5, h6");
        List<HeadingInfo> headings = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (Element headingElement : headingElements) {
            String text = headingElement.text();
            String anchorId = buildAnchorId(text, counter.incrementAndGet());
            int level = Integer.parseInt(headingElement.tagName().substring(1));
            headingElement.attr("id", anchorId);
            headings.add(new HeadingInfo(text, anchorId, level));
        }
        return new ProcessedHtml(document.body().html(), headings);
    }

    private List<TocItem> toTree(List<HeadingInfo> headings) {
        TocItem rootItem = new TocItem("ROOT", "root", 0, new ArrayList<>());
        Deque<TocItem> stack = new ArrayDeque<>();
        stack.push(rootItem);

        for (HeadingInfo heading : headings) {
            TocItem current = new TocItem(heading.text(), heading.anchorId(), heading.level(), new ArrayList<>());
            while (stack.peek().level() >= current.level()) {
                stack.pop();
            }
            stack.peek().children().add(current);
            stack.push(current);
        }

        return rootItem.children();
    }

    private String buildAnchorId(String source, int index) {
        String normalized = NON_ALPHANUMERIC.matcher(source.toLowerCase(Locale.ROOT)).replaceAll("-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            normalized = "section";
        }
        return normalized + "-" + index;
    }

    private record HeadingInfo(String text, String anchorId, int level) {
    }

    private record ProcessedHtml(String htmlBody, List<HeadingInfo> headings) {
    }
}
