package com.example.mdreader.util;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResourceResolver {
    private static final Pattern IMAGE_SRC_PATTERN = Pattern.compile("src\\s*=\\s*\"(.*?)\"");
    private static final Pattern LINK_HREF_PATTERN = Pattern.compile("href\\s*=\\s*\"(.*?)\"");

    private ResourceResolver() {
    }

    public static String rewriteRelativeResources(String html, Path baseDir) {
        if (html == null || baseDir == null) {
            return html;
        }
        String withImages = rewriteAttribute(html, IMAGE_SRC_PATTERN, baseDir);
        return rewriteAttribute(withImages, LINK_HREF_PATTERN, baseDir);
    }

    private static String rewriteAttribute(String html, Pattern pattern, Path baseDir) {
        Matcher matcher = pattern.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String original = matcher.group(1);
            String replacement = original;
            if (shouldRewrite(original)) {
                replacement = baseDir.resolve(original).normalize().toUri().toString();
            }
            matcher.appendReplacement(buffer, matcher.group().replace(original, Matcher.quoteReplacement(replacement)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static boolean shouldRewrite(String value) {
        return !(value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("file:/")
                || value.startsWith("#")
                || value.startsWith("mailto:"));
    }
}
