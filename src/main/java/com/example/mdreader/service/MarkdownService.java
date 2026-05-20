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
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ast.Heading;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownService {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern HEADING_TAG_PATTERN = Pattern.compile("<h([1-6])(.*?)>");

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
        List<HeadingInfo> headings = extractHeadings(document);
        List<TocItem> tocItems = toTree(headings);
        String html = injectHeadingAnchors(renderer.render(document), headings);
        String resolvedHtml = ResourceResolver.rewriteRelativeResources(html, source.baseDir());
        String title = !tocItems.isEmpty() ? tocItems.getFirst().text() : source.fileName();

        return new RenderedDocument(title, resolvedHtml, tocItems, source.path(), source.baseDir());
    }

    private List<HeadingInfo> extractHeadings(Document document) {
        List<HeadingInfo> headings = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        collectHeadings(document, headings, counter);
        return headings;
    }

    private void collectHeadings(Node node, List<HeadingInfo> headings, AtomicInteger counter) {
        for (Node current = node.getFirstChild(); current != null; current = current.getNext()) {
            if (current instanceof Heading heading) {
                String text = heading.getText().toString();
                String anchor = buildAnchorId(text, counter.incrementAndGet());
                headings.add(new HeadingInfo(text, anchor, heading.getLevel()));
            }
            collectHeadings(current, headings, counter);
        }
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

    private String injectHeadingAnchors(String html, List<HeadingInfo> headings) {
        if (headings.isEmpty()) {
            return html;
        }

        Matcher matcher = HEADING_TAG_PATTERN.matcher(html);
        StringBuffer buffer = new StringBuffer();
        int index = 0;
        while (matcher.find()) {
            if (index >= headings.size()) {
                break;
            }

            String attributes = matcher.group(2);
            String anchorId = headings.get(index).anchorId();
            String replacementTag;
            if (attributes.contains(" id=")) {
                replacementTag = "<h" + matcher.group(1) + attributes + ">";
            } else {
                replacementTag = "<h" + matcher.group(1) + attributes + " id=\"" + anchorId + "\">";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacementTag));
            index++;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private record HeadingInfo(String text, String anchorId, int level) {
    }
}
