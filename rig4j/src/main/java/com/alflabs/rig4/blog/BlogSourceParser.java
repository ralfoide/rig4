package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.HtmlTransformer;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a blog page (from GDoc's HTML export) and returns:
 * - The header tags from the first line.
 * - The izu:blog tag must be present and indicates the start of the blog content.
 * - Ensures the blog has an izu:cat(egory) defined.
 * - The izu:header if present.
 * - A list of posts' contents with date, title and 2 variations: short (optional) & full.
 */
public class BlogSourceParser {

    private final HtmlTransformer mHtmlTransformer;

    /**
     * Matches an Izu tag.
     * - Group 1 is the opening tag ([ or [[).
     * - Group 2 is the izu tag with the "izu:" part, without the brackets.
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

    public ParsedResult parse(byte[] content) throws IOException, URISyntaxException {
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
        boolean inHeaderTags = true;
        String blogCategory = null;
        List<String> headerTags = null;
        Element izuHeaderStart = null;
        Element izuHeaderEnd = null;
        ElementSection currentSection = null;
        List<ParsedSection> parsedSections = new ArrayList<>();

        for (Element element : body.children()) {
            List<String> izuTags = parseIzuTags(element);

            if (!foundIzuBlogTag) {
                if (izuTags != null && izuTags.contains(IzuTags.IZU_BLOG)) {
                    foundIzuBlogTag = true;
                    headerTags = izuTags;
                    izuHeaderStart = element;

                    blogCategory = IzuTags.getTagValue(IzuTags.IZU_CATEGORY, izuTags);
                    if (blogCategory.isEmpty()) {
                        throw new ParseException(
                                "Missing " + IzuTags.IZU_CATEGORY + "...] on the " + IzuTags.IZU_BLOG + " line");
                    }
                }
                continue;
            }

            if (inHeaderTags) {
                if (izuTags == null || izuTags.isEmpty()) {
                    inHeaderTags = false;
                } else {
                    headerTags.addAll(izuTags);
                    continue;
                }
            }

            if (izuHeaderEnd == null && izuTags != null && izuTags.contains(IzuTags.IZU_HEADER_END)) {
                izuHeaderEnd = element;
            }

            if (currentSection != null && currentSection.start == null) {
                currentSection.start = element;
            }

            if (currentSection != null && currentSection.start != null
                    && currentSection.break_ == null
                    && izuTags != null && izuTags.contains(IzuTags.IZU_BREAK)) {
                currentSection.break_ = element;
            }

            ElementSection newSection = parseSectionTag(element);
            if (newSection != null) {
                if (currentSection != null) {  // commit last section if any
                    parsedSections.add(generateSection(currentSection));
                }
                currentSection = newSection;
            }

            if (currentSection != null) {
                currentSection.end = element;

                if (izuTags != null) {
                    currentSection.addTags(izuTags);
                }
            }

            if (izuTags != null && izuTags.contains(IzuTags.IZU_BLOG_END)) {
                break;
            }
        }

        if (currentSection != null) {  // commit last section if any
            parsedSections.add(generateSection(currentSection));
        }

        Element headerElement = combineElements(izuHeaderStart, izuHeaderEnd);
        return new ParsedResult(blogCategory, headerTags, headerElement, parsedSections);
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
            section.tagS = matcher.group(2);
            section.afterTagS = text.substring(matcher.end());
            section.title = element;
            return section;
        }
        return null;
    }

    private ParsedSection generateSection(ElementSection section) throws ParseException {
        LocalDate date;
        String textTitle = "";
        Element intermediaryShort;
        Element intermediaryfull;

        String tagS = section.tagS;
        int pos = tagS.indexOf(":");
        if (pos > 0) {
            textTitle = tagS.substring(pos + 1);
            tagS = tagS.substring(0, pos);
        }

        // Parse date.
        try {
            date = LocalDate.parse(tagS.trim(), DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new ParseException("Date parsing failed. " + e.getMessage());
        }

        // Parse title from tag.
        if (!textTitle.isEmpty()) {
            textTitle = textTitle.trim();
        }

        // If not from tag, parse title from text.
        if (textTitle.isEmpty()) {
            textTitle = section.afterTagS.trim();
        }

        // Get the content.
        intermediaryfull = combineElements(section.start, section.end);

        if (section.break_ != null) {
            intermediaryShort = combineElements(section.start, section.break_);
        } else {
            intermediaryShort = combineElements(null, null);
        }

        return new ParsedSection(date, textTitle, intermediaryShort, intermediaryfull, section.izuTags);
    }

    @Null
    private Element combineElements(@Null Element start, @Null Element end) {
        if (start == null || end == null) {
            return null;
        }
        Element div = new Element(HtmlTransformer.ELEM_DIV);
        Node node = start;
        while (node != null) {
            div.appendChild(node.clone());
            if (node == end) {
                break;
            }
            node = node.nextSibling();
        }

        return div;
    }

    /** Internal temporary structure accumulated as the source HTML gets parsed. */
    private static class ElementSection {
        String tagS;
        String afterTagS;
        List<String> izuTags;
        Element title;
        Element start;
        Element break_;
        Element end;

        public void addTags(List<String> izuTags) {
            if (this.izuTags == null) {
                this.izuTags = new ArrayList<>();
            }
            this.izuTags.addAll(izuTags);
        }
    }

    /** A {@link BlogSourceParser} exception. */
    public static class ParseException extends IOException {
        public ParseException() {
            super();
        }

        public ParseException(String message) {
            super(message);
        }
    }

    /**
     * Represents one parsed source file with its tags, optional header and sections.
     */
    public static class ParsedResult {
        private final String mBlogCategory;
        private final List<String> mTags;
        private final Element mIntermediaryHeader;
        private final List<ParsedSection> mParsedSections;
        private String mSourceReference;
        private boolean mFileChanged;

        public ParsedResult(
                @NonNull String blogCategory,
                @NonNull List<String> tags,
                @Null Element intermediaryHeader,
                @NonNull List<ParsedSection> parsedSections) {
            mBlogCategory = blogCategory;
            mTags = Collections.unmodifiableList(tags);
            mIntermediaryHeader = intermediaryHeader;
            mParsedSections = parsedSections == null
                    ? Collections.EMPTY_LIST
                    : Collections.unmodifiableList(parsedSections);
        }

        public boolean isFileChanged() {
            return mFileChanged;
        }

        public String getSourceReference() {
            return mSourceReference;
        }

        public ParsedResult setFileChanged(String sourceReference, boolean fileChanged) {
            mSourceReference = sourceReference;
            mFileChanged = fileChanged;
            return this;
        }

        @NonNull
        public String getBlogCategory() {
            return mBlogCategory;
        }

        /** Tags should at least contain [blog], and typically [cat:...] */
        @NonNull
        public List<String> getTags() {
            return mTags;
        }

        /** Optional HTML-formatted header. Can be null. */
        @Null
        public Element getIntermediaryHeader() {
            return mIntermediaryHeader;
        }

        /** HTML-formatted sections (short & full), with date & title. Can be empty but not null. */
        public List<ParsedSection> getParsedSections() {
            return mParsedSections;
        }
    }

    /**
     * Represents one section from the a parsed blog with its date/title and formatted content.
     */
    public static class ParsedSection {
        private final List<String> mIzuTags;
        private final LocalDate mDate;
        private final String mTextTitle;
        private final Element mIntermediaryShort;
        private final Element mIntermediaryfull;

        public ParsedSection(
                @NonNull LocalDate date,
                @NonNull String textTitle,
                @NonNull Element intermediaryShort,
                @NonNull Element intermediaryfull,
                @Null List<String> izuTags) {
            mDate = date;
            mTextTitle = textTitle;
            mIntermediaryShort = intermediaryShort;
            mIntermediaryfull = intermediaryfull;
            mIzuTags = izuTags == null ? Collections.emptyList() : izuTags;
        }

        /** Date extracted from the title. */
        @NonNull
        public LocalDate getDate() {
            return mDate;
        }

        /**
         * Title either from [s] tag or from the title line. Title is not part of formatted content.
         */
        @NonNull
        public String getTextTitle() {
            return mTextTitle;
        }

        /**
         * Optional <em>intermediary</em> HTML-formatted short content.
         * <p/>
         * Some elements have not been cleaned up and {@link HtmlTransformer.LazyTransformer}
         * must be called on this result before using it.
         * <p/>
         * Can be empty or null.
         */
        @Null
        public Element getIntermediaryShort() {
            return mIntermediaryShort;
        }

        /**
         * <em>Intermediary</em> HTML-formatted "full" content.
         * When the post does not have a short vs expanded content, all the content is "full".
         * <p/>
         * Some elements have not been cleaned up and {@link HtmlTransformer.LazyTransformer}
         * must be called on this result before using it.
         * <p/>
         * Can not be empty neither null.
         */
        @NonNull
        public Element getIntermediaryfull() {
            return mIntermediaryfull;
        }

        /** Optional tags defined while parsing this section. */
        @NonNull
        public List<String> getIzuTags() {
            return mIzuTags;
        }
    }
}
