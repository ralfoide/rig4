package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.HtmlTransformer;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a blog page and returns:
 * - The header tags from the first line.
 * - The izu:blog tag must be present and indicates the start of the blog content.
 * - The izu:header if present.
 * - A list of posts' contents with date, title and 2 variations: summary & full.
 */
public class BlogSourceParser {

    private final HtmlTransformer mHtmlTransformer;

    /**
     * Matches an Izu tag.
     * - Group 1 is the opening tag ([ or [[).
     * - Group 2 is the izu tag without the [izu:...] part.
     */
    public final static Pattern RE_IZU_TAG = Pattern.compile("(\\[\\[?)(izu[:-][^\\[\\]]+)]");
    /**
     * Matches an section [s:] tag.
     * - Group 1 is the opening tag ([ or [[).
     * - Group 2 is the tag without the [s:...] part.
     */
    public final static Pattern RE_S_TAG = Pattern.compile("(\\[\\[?)s:([^\\[\\]]+)]");

    public BlogSourceParser(HtmlTransformer htmlTransformer) {
        mHtmlTransformer = htmlTransformer;
    }

    public void parse(byte[] content) throws IOException, URISyntaxException {
        Element body = mHtmlTransformer.simplifyForProcessing(content);

        // Iterate on the top-level elements and keeps a state:
        // - Blog parsing starts as soon as [izu:blog] is found.
        // - Everything after till the first [s:] or [izu:header:end] is the header.
        // - A new section each time [s:] is seen. It's a parse error if something looks like
        //      an [s:] tag but isn't.
        // - We may find an [izu:break] inside a section.
        // - Parse ends when [izu:blog:end] is found.
        // - Allow overrides using the [[s:] or [[izu:] syntax.
        boolean foundIzuBlogTag = false;
        List<String> headerTags = null;
        Element izuHeaderStart = null;
        Element izuHeaderEnd = null;
        ElementSection currentSection = null;

        for (Element element : body.children()) {
            List<String> izuTags = parseIzuTags(element);

            if (!foundIzuBlogTag) {
                if (izuTags != null && izuTags.contains("blog")) {
                    foundIzuBlogTag = true;
                    headerTags = izuTags;
                    izuHeaderStart = element;
                }
                continue;
            }
            if (izuHeaderEnd == null && izuTags != null && izuTags.contains("header:end")) {
                izuHeaderEnd = element;
            }

            if (currentSection != null && currentSection.start == null) {
                currentSection.start = element;
            }

            ElementSection newSection = parseSectionTag(element);
            if (newSection != null) {
                currentSection = newSection;
            }

            if (currentSection != null) {
                currentSection.end = element;
            }

            if (izuTags != null && izuTags.contains("blog:end")) {
                break;
            }
        }
    }

    @Null
    private List<String> parseIzuTags(@NonNull Element element) {
        List<String> tags = null;
        String text = element.text(); // this include all children
        Matcher matcher = RE_IZU_TAG.matcher(text);
        while (matcher.find()) {
            if (matcher.group(1).length() > 1) {
                // skip tags starting with [[
                continue;
            }
            if (tags == null) {
                tags = new ArrayList<>();
            }
            tags.add(matcher.group(2));
        }
        return tags;
    }

    @Null
    private ElementSection parseSectionTag(@NonNull Element element) {
        String text = element.text(); // this include all children
        Matcher matcher = RE_S_TAG.matcher(text);
        if (matcher.find() && matcher.group(1).length() == 1) {
            ElementSection section = new ElementSection();
            section.tag = matcher.group(2);
            section.afterTag = text.substring(matcher.end());
            section.title = element;
            return section;
        }
        return null;
    }

    private static class ElementSection {
        String tag;
        String afterTag;
        Element title;
        Element start;
        Element end;
    }

    private static class ParsedResult {
        /** Tags should at least contain [blog], and typically [cat:...] */
        List<String> mTags;
        /** Optional HTML-formatted header. Can be null. */
        String mFormattedHeader;
        /** HTML-formatted sections (short & full), with date & title. */
        List<ParsedSection> mParsedSections;
    }

    private static class ParsedSection {
        /** Date extracted from the title. */
        Date mDate;
        /** Title either from [s] tag or from the title line. Title is not part of formatted content. */
        String mTextTitle;
        /** Optional HTML-formatted short content. Can be null. */
        String mFormattedShort;
        /** HTML-formatted full content. */
        String mFormattedFull;
    }
}
