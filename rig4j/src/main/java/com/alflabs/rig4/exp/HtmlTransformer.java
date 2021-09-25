package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.blog.BlogSourceParser;
import com.alflabs.rig4.blog.IzuTags;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.RPair;
import com.alflabs.utils.RSparseArray;
import com.google.common.base.Charsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeTraversor;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

public class HtmlTransformer {

    private static final String ELEM_A = "a";
    public  static final String ELEM_DIV = "div";
    private static final String ELEM_HR = "hr";
    private static final String ELEM_IFRAME = "iframe";
    private static final String ELEM_IMG = "img";
    private static final String ELEM_LI = "li";
    private static final String ELEM_P = "p";
    private static final String ELEM_TD = "td";
    private static final String ELEM_SPAN = "span";
    private static final String ELEM_STYLE = "style";
    private static final String ELEM_UL = "ul";

    private static final String ATTR_ALT= "alt";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_HREF = "href";
    private static final String ATTR_ID = "id";
    private static final String ATTR_SRC = "src";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_WIDTH = "width";
    private static final String ATTR_HEIGHT = "height";

    private static final String QUERY_Q = "q";
    private static final String QUERY_W = "w";
    private static final String QEURY_H = "h";
    private static final String QUERY_RIG4EMBED = "rig4embed";

    private static final String FONT_CONSOLE = "Consolas";
    private static final String CLASS_CONSOLE = "console";

    private static final String HTML_NBSP = Entities.getByName("nbsp");
    private final Flags mFlags;
    private final HashStore mHashStore;
    private final Timing.TimeAccumulator mTiming;

    @Inject
    public HtmlTransformer(
            Flags flags,
            Timing timing,
            HashStore hashStore) {
        mFlags = flags;
        mHashStore = hashStore;
        mTiming = timing.get("HtmlTransformer");
    }

// Obsolete. Replaced by simplifyForProcessing + LazyTransformer.
//    /**
//     * Simplifies a GDoc exported HTML.
//     * Returns the <em>Body</em> element only, usable for HTML export.
//     * This removes all Izu tags and transforms links, and images.
//     */
//    public String simplifyForHtml(@NonNull byte[] content, @NonNull Callback callback)
//            throws IOException, URISyntaxException {
//        mTiming.start();
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
//            Document doc = Jsoup.parse(bais, null /* charset */, "" /* base uri */);
//
//            doc = cleanup(doc);
//            removeEmptyElements(doc, ELEM_A);
//            removeEmptyElements(doc, ELEM_SPAN);
//            rewriteBulletLists(doc);
//            cleanupLineStyle(doc);
//            cleanupConsolasLineStyle(doc);
//            cleanupInlineStyle(doc);
//            rewriteUrls(doc, ATTR_HREF, callback, transformKey);
//            rewriteUrls(doc, ATTR_SRC, callback, transformKey);
//            rewriteYoutubeEmbed(doc);
//            linkifyImages(doc);
//            removeIzuComments(doc);
//            removeIzuTags(doc);
//
//            doc.outputSettings().prettyPrint(true);
//            doc.outputSettings().charset(Charsets.UTF_8);
//
//            // -- for debugging -- return doc.html();
//            Element body = doc.getElementsByTag("body").first();
//            return body.html();
//        } finally {
//            mTiming.end();
//        }
//    }

    /**
     * Simplifies a GDoc exported HTML.
     * Returns the <em>Body</em> element only for intermediate processing.
     *
     * This does NOT removes Izu tags and does not transforms links, and images.
     * The output is designed to be given to {@link LazyTransformer} later.
     */
    public Element simplifyForProcessing(@NonNull byte[] content)
            throws IOException, URISyntaxException {
        mTiming.start();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            Document doc = Jsoup.parse(bais, null /* charset */, "" /* base uri */);

            doc = cleanup(doc);
            removeEmptyElements(doc, ELEM_A);
            removeEmptyElements(doc, ELEM_SPAN);
            rewriteBulletLists(doc);
            cleanupLineStyle(doc);
            cleanupConsolasLineStyle(doc);
            cleanupInlineStyle(doc);

            doc.outputSettings().prettyPrint(true);
            doc.outputSettings().charset(Charsets.UTF_8);

            return doc.getElementsByTag("body").first();
        } finally {
            mTiming.end();
        }
    }

    /**
     * Finish processing content extracted using {@link #simplifyForProcessing(byte[])}.
     */
    public interface LazyTransformer {
        /**
         * A key tying the transformer to the output (typically the generated file path or a hash
         * of it or a blog category). Next time the transformer is called, any cached generated
         * output should be ignored if the transformer key has changed. This would happen when
         * a post's content is used in multiple blogs. All intermediary asset files typically
         * need to be generated for each one.
         */
        @NonNull
        String getTransformKey();

        /** Transforms the HTML element into the desired HTML text. */
        @Null
        Element lazyTransform(@Null Element element) throws IOException, URISyntaxException;

        /**
         * Extracts the source of the first img tag of the content.
         * <p/>
         * The src attribute will only be correct with elements that have already been processed by
         * {@link #lazyTransform(Element)} since it needs to rewrite the URLs first.
         */
        @Null
        String getFormattedFirstImageSrc(@Null Element formatted);

        /**
         * Extracts the source of the first paragraph of the content. When content is returned,
         * it is stripped of any html and is "pure text".
         * <p/>
         * This only works with elements that have already been processed by
         * {@link #lazyTransform(Element)} so that all content has been properly rewritten
         * and cleaned.
         */
        @Null
        String getFormattedDescription(@Null Element formatted);
    }

    /**
     * Finish processing content extracted using {@link #simplifyForProcessing(byte[])}.
     *
     * @param transformKey Transformer key is the directory of the file generated.
     *                     If a post is reused in a different directory, its assets should be
     *                     regenerated for that directory.
     * @param callback Callbacks to transform images and drawings.
     */
    public LazyTransformer createLazyTransformer(@NonNull String transformKey, @NonNull Callback callback) {
        return new LazyTransformer() {
            @Override
            @NonNull
            public String getTransformKey() {
                return transformKey;
            }

            @Override
            @Null
            public Element lazyTransform(@Null Element element) throws IOException, URISyntaxException {
                if (element == null) {
                    return null;
                }

                element = element.clone();
                rewriteUrls(element, ATTR_HREF, callback, transformKey);
                rewriteUrls(element, ATTR_SRC, callback, transformKey);
                rewriteYoutubeEmbed(element);
                linkifyImages(element);
                removeIzuComments(element);
                removeIzuTags(element);
                return element;
            }

            @Override
            @Null
            public String getFormattedFirstImageSrc(@Null Element element) {
                if (element == null) {
                    return null;
                }

                // Find the first <img> with a src attribute that is not empty.
                for (Element img : element.getElementsByTag(ELEM_IMG)) {
                    String src = img.attr(ATTR_SRC);
                    if (!src.isEmpty()) {
                        return src;
                    }
                }

                return null;
            }

            @Override
            @Null
            public String getFormattedDescription(@Null Element element) {
                if (element == null) {
                    return null;
                }

                // Find the first paragraph with a content that is not empty once cleaned up.
                for (Element p : element.getElementsByTag(ELEM_P)) {
                    if (p.childNodeSize() == 0) {
                        continue;
                    }

                    Document dirtyDoc = Document.createShell("");
                    Element body = dirtyDoc.body();
                    body.appendChild(p.clone());

                    Cleaner cleaner = new Cleaner(Whitelist.none());
                    Document cleanDoc = cleaner.clean(dirtyDoc);
                    String cleaned = cleanDoc.body().html().trim();

                    if (!cleaned.isEmpty()) {
                        return cleaned;
                    }
                }

                return null;
            }
        };
    }

    /**
     * Use Jsoup's cleaner to remove all tags not in the "relaxed" group.
     * Things we keep for gdoc are HR and STYLE tags as well as all STYLE attributes.
     */
    private Document cleanup(Document doc) {
        Whitelist relaxed = Whitelist.relaxed();
        relaxed.preserveRelativeLinks(true);
        relaxed.addTags(ELEM_HR);
        relaxed.addTags(ELEM_STYLE);
        relaxed.addProtocols(ELEM_A, ATTR_HREF, "#"); // allow internal anchors
        relaxed.addAttributes(":all", ATTR_ID);
        relaxed.addAttributes(":all", ATTR_STYLE);
        Cleaner cleaner = new Cleaner(relaxed);
        doc = cleaner.clean(doc);
        return doc;
    }

    /**
     * Remove all elements which have no child.
     * The GDoc export generates quite a number of useless ones.
     */
    private void removeEmptyElements(Element root, String name) {
        for (Element element : root.getElementsByTag(name)) {
            if (element.childNodeSize() == 0) {
                element.remove();
            }
        }
    }

    private void removeIzuComments(Element root) {

        NodeFilter filter = new NodeFilter() {
            boolean removing = false;

            @Override
            public FilterResult head(Node node, int depth) {
                if (node instanceof Element) {
                    String subText = ((Element) node).text();
                    String ownText = ((Element) node).ownText();
                    if (removing) {
                        if (subText.contains("--]")) {
                            removing = false;
                            return FilterResult.REMOVE;
                        }
                    } else {
                        if (ownText.contains("[!--")) {
                            removing = !subText.contains("--]");
                            return FilterResult.REMOVE;
                        }
                    }
                }
                return removing ? FilterResult.REMOVE : FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        };
        NodeTraversor.filter(filter, root);
    }

    /**
     * Remove all [izu...] references in the text. I want to be able to add these in
     * current gdocs and not show them by mistake in the final output.
     */
    private void removeIzuTags(Element root) {
        // Note: "[izu" is _not_ the IzuTags.Prefix as it doesn't use the trailing colon (':').
        for (Element element : root.getElementsContainingOwnText("[izu")) {
            for (int i = 0, n = element.childNodeSize(); i < n; i++) {
                Node node = element.childNode(i);
                if (node instanceof TextNode) {
                    String text = ((TextNode) node).getWholeText();
                    Matcher matcher = BlogSourceParser.RE_IZU_TAG.matcher(text);
                    text = matcher.replaceAll("");
                    ((TextNode) node).text(text);
                }
            }
        }
    }

    /** Same as previousElementSibling() but with a deep search in children. */
    @Null
    private Element findPreviousElementSibling(@NonNull Element element) {
        Element sibling = element.previousElementSibling();
        if (sibling == null) {
            return element.parent();
        }
        while (true) {
            Elements children = sibling.children();
            if (children.isEmpty()) {
                break;
            }
            sibling = children.last();
        }
        return sibling;
    }

    @SuppressWarnings("UnnecessaryLabelOnContinueStatement")
    private void linkifyImages(Element root) {
        Set<Element> visited = new HashSet<>();
        nextIzu: for (Element element : root.getElementsContainingOwnText("[" + IzuTags.IZU_LINK_IMG)) {
            visited.add(element);

            String text = "";
            String href = "";
            Matcher matcher = BlogSourceParser.RE_IZU_TAG.matcher(element.text());
            if (matcher.matches()) {
                String tag = matcher.group(2);
                if (tag.length() > IzuTags.IZU_LINK_IMG.length()) {
                    href = tag.substring(IzuTags.IZU_LINK_IMG.length()).trim();
                }
            }

            // Find previous <img>
            Element img = findPreviousElementSibling(element);
            while (img != null) {
                if (visited.contains(img)) {
                    continue nextIzu;
                }
                if (img.tagName().equals(ELEM_IMG)) {
                    break;
                }
                img = findPreviousElementSibling(img);
            }

            if (img == null) {
                continue nextIzu;
            }

            if (href.isEmpty()) {
                // Find previous <a href> link
                Element link = findPreviousElementSibling(img);
                while (link != null && !link.tagName().equals(ELEM_A)) {
                    link = findPreviousElementSibling(link);
                }

                if (link == null) {
                    continue nextIzu;
                }

                text = link.text();
                href = link.attr(ATTR_HREF);

                if (href.isEmpty() || text.isEmpty()) {
                    continue nextIzu;
                }

                visited.add(link);
            }

            visited.add(img);

            Element newLink = new Element(ELEM_A);
            newLink.attr(ATTR_HREF, href);
            img.before(newLink);
            newLink.insertChildren(0, img);

            if (!text.isEmpty()) {
                img.attr(ATTR_ALT, text);
                img.attr(ATTR_TITLE, text);
            }
        }
    }

    /**
     * Cleanup all STYLE attributes in 2 pases:
     * - The first P/SPAN is located and acts as a markers for the "default paragraph".
     *   A simple normal-style line can be added for that purpose as the very first line in the doc.
     * - This method descends all elements recursively, only keeping the style that is a delta with
     *   the parents (we simplify and assume they are all inherited from their parent).
     *
     * Step 2 alone removes a ton of useless style info which is endlessly repeated by the
     * gdoc exporter while still diffing the styles against a (supposedly) stable reference.
     *
     * Combined with step 1 above, it also means all the "normal" style attributes are wiped out
     * and the style from the css can be respected.
     */
    private void cleanupInlineStyle(Element root) {
        Element p = root.getElementsByTag(ELEM_P).first();
        CssStyles eraseStyles = new CssStyles(p == null ? null : p.attr(ATTR_STYLE));
        // mark these as part of the baseline to get rid of
        eraseStyles.add("height:11pt");
        eraseStyles.add("font-family:\"Arial\"");
        eraseStyles.add("font-style:normal");
        eraseStyles.add("font-weight:400");
        eraseStyles.add("text-decoration:none");
        eraseStyles.add("vertical-align:baseline");
        // this one is the default in the css so let's have it erased too
        eraseStyles.add("text-align:justify");

        cleanupInlineStyleRecursive(root, eraseStyles);
    }

    private void cleanupInlineStyleRecursive(Element root, CssStyles parentStyles) {
        for (int i = 0, n = root.childNodeSize(); i < n; i++) {
            Node node = root.childNode(i);
            if (node instanceof Element) {
                Element element = (Element) node;

                if (!element.hasAttr(ATTR_STYLE)) {
                    cleanupInlineStyleRecursive(element, parentStyles);
                    continue;
                }

                RPair<CssStyles, String> pair = parentStyles.deltaChildStyle(element.attr(ATTR_STYLE));
                CssStyles newParentStyles = pair == null ? parentStyles : pair.first;
                String newStyle = pair == null ? "" : pair.second;
                if (newStyle.isEmpty()) {
                    element.removeAttr(ATTR_STYLE);
                } else {
                    element.attr(ATTR_STYLE, newStyle);
                }
                cleanupInlineStyleRecursive(element, newParentStyles);
            }
        }
    }

    /**
     * Cleanup a lot of padding style attributes that don't seem that useful.
     */
    private void cleanupLineStyle(Element root) {
        for (Element element : root.getElementsByTag(ELEM_P)) {
            CssStyles styles = new CssStyles(element.attr(ATTR_STYLE));
            styles.remove("padding-bottom");
            styles.remove("padding-left");
            styles.remove("padding-right");
            styles.remove("padding-top");
            styles.remove("padding");
            styles.applyTo(element);
        }

    }

    /**
     * Cleanup the "source code" tables in the gdoc exported html. This relies on my
     * convention to include source code in these documents: They use a Consolas font,
     * size 10, and are always inserted into a single cell table (one row, one column).
     *
     * The generated code does not use PRE (understandably). Instead one P + SPAN is generated
     * for each line with text. Empty lines use a P only with a fixed height.
     *
     * The cleanup works as follows:
     * - The template CSS has a "console" class applied to P that removes the margin after P.
     * - P elements have the class "console" applied if they are followed by a SPAN that has a
     *   style listing Consolas (for simplification I just check that name, not the whole font
     *   family attribute). This would cover the Consolas usage when it is also done outside
     *   a table.
     * - For all the TR > TD > P > SPAN structures, find the TD, and in there make sure there
     *   is at least one SPAN with a Consolas style. If we find that, capture that style.
     *   For all the P elements, remove any height style attribute and instead add a SPAN
     *   with an nbsp if the P has no child (otherwise they show up as zero size). And also
     *   make sure they are tagged with the console CSS class.
     */
    private void cleanupConsolasLineStyle(Element root) {
        for (Element span : root.select("p > span")) {
            String spanStyle = span.attr(ATTR_STYLE);
            if (spanStyle != null && spanStyle.contains(FONT_CONSOLE)) {
                Element p = span.parent();
                if (p != null && p.tagName().equals(ELEM_P)) {
                    String clazz = p.attr(ATTR_CLASS);
                    if (!clazz.contains(CLASS_CONSOLE)) {
                        p.attr(ATTR_CLASS, CLASS_CONSOLE + " " + clazz);
                    }
                }
            }
        }

        Set<Element> visited = new HashSet<>();
        for (Element span1 : root.select("tr > td > p > span")) {
            Element p1 = span1.parent();
            Element td = p1 == null ? null : p1.parent();
            if (td == null || visited.contains(td)) {
                continue;
            }

            visited.add(td);

            String consoleSpanStyle = null;
            for (Element span2 : td.select("p > span")) {
                String spanStyle = span2.attr(ATTR_STYLE);
                if (spanStyle != null && spanStyle.contains(FONT_CONSOLE)) {
                    consoleSpanStyle = spanStyle;
                    break;
                }
            }

            if (consoleSpanStyle == null) {
                continue;
            }

            for (Element p2 : td.getElementsByTag(ELEM_P)) {
                CssStyles styles = new CssStyles(p2.attr(ATTR_STYLE));
                if (styles.has(ATTR_HEIGHT)) {
                    styles.remove(ATTR_HEIGHT);
                    styles.applyTo(p2);
                }
                String clazz = p2.attr(ATTR_CLASS);
                if (!clazz.contains(CLASS_CONSOLE)) {
                    p2.attr(ATTR_CLASS, CLASS_CONSOLE + " " + clazz);
                }
                if (p2.childNodeSize() == 0) {
                    p2.appendElement(ELEM_SPAN)
                            .attr(ATTR_STYLE, consoleSpanStyle)
                            .appendText(HTML_NBSP);
                }
            }
        }
    }

    /**
     * Rewrite UL / LI lists.
     *
     * This works by measuring the margin-left in UL > LI and using that to recreate the proper
     * nesting.
     *
     * First find an UL tag with some LI.
     * For any UL tag immediately after (next sibling), move its LI at the end of the first UL.
     *
     * That will give us one UL instead of many per level and all the LI at the same level are
     * not differentiated by their margin-left.
     *
     * Instead of simply adding the LI into the first UL, look at the margin-left values.
     * Value should not be considered absolute but relative.
     * When values are larger, create a new UL to nest values.
     * When the values become smaller, go find a previous level to append to.
     * Use a lookup table to memorize which margin-left matches which nested UL level,
     * in case a bullet list goes from level N to level N-2 or more directly.
     */
    private void rewriteBulletLists(Element root) {
        Set<Element> visitedUl = new HashSet<>();

        restartRootUl: while (true) {
            for (Element ul1 : root.getElementsByTag(ELEM_UL)) {
                if (visitedUl.contains(ul1)) {
                    continue;
                }
                visitedUl.add(ul1);

                Set<Element> removeUl = new HashSet<>();
                ArrayList<Element> moveLi = new ArrayList<>();
                Element ul2 = ul1;
                nextSiblingUl:
                while (ul2 != null && ul2.tagName().equals(ELEM_UL)) {
                    // Check that UL contains *only* LI elements. If it doesn't then abort.
                    for (Element li : ul2.children()) {
                        if (!li.tagName().equals(ELEM_LI)) {
                            break nextSiblingUl;
                        }
                    }

                    for (Element li : ul2.children()) {
                        moveLi.add(li.clone());
                    }

                    if (ul2 != ul1) {
                        removeUl.add(ul2);
                        visitedUl.add(ul2);
                    }

                    ul2 = ul2.nextElementSibling();
                }

                if (moveLi.isEmpty()) {
                    continue; // next root UL
                }

                for (Element ul3 : removeUl) {
                    ul3.remove();
                }

                for (Element li1 : ul1.children()) {
                    li1.remove();
                }

                CssStyles ulStyles = new CssStyles(ul1.attr(ATTR_STYLE));
                ulStyles.remove("padding");
                ulStyles.applyTo(ul1);

                int lastMarginLeft = 0;
                Element currLevel = ul1;
                RSparseArray<Element> levelMap = new RSparseArray<>();
                levelMap.put(lastMarginLeft, currLevel);
                for (Element li1 : moveLi) {
                    CssStyles liStyles = new CssStyles(li1.attr(ATTR_STYLE));
                    int marginLeft = liStyles.getIntValue("margin-left", lastMarginLeft);
                    liStyles.remove("margin-left");
                    liStyles.remove("margin-right");
                    liStyles.remove("margin-top");
                    liStyles.remove("margin-bottom");
                    liStyles.remove("padding");
                    liStyles.applyTo(li1);

                    if (marginLeft > lastMarginLeft && lastMarginLeft > 0) {
                        lastMarginLeft = marginLeft;
                        currLevel = currLevel.appendElement(ELEM_UL);
                        levelMap.put(marginLeft, currLevel);
                    } else if (marginLeft < lastMarginLeft) {
                        if (marginLeft < 0) {
                            marginLeft = 0;
                        }
                        lastMarginLeft = marginLeft;
                        currLevel = levelMap.get(marginLeft);
                        if (currLevel == null) {
                            int val = -1;
                            for (int i = 0; i < levelMap.size() && val < marginLeft; i++) {
                                val = levelMap.keyAt(i);
                                currLevel = levelMap.get(val);
                            }
                        }

                    } else if (levelMap.get(marginLeft) == null) {
                        lastMarginLeft = marginLeft;
                        levelMap.put(marginLeft, currLevel);
                    }

                    currLevel.appendChild(li1);
                }

                continue restartRootUl;
            }
            break;
        }
    }

    /**
     * Rewrite URLs:
     * - The gdoc exporter wraps all URLs using the google URL redirectory. Simply refer to the
     *   source directly and bypass the redirector.
     * - Handle drawing exported PNGs links by downloading them and rewriting them locally.
     * - Any untreated google.com link is an error that should be loooked into.
     */
    private void rewriteUrls(Element root, String attrName, Callback callback, String transformKey)
            throws IOException, URISyntaxException {

        String contentHash = DigestUtils.shaHex(root.text());
        String contentKey = String.format("rewrite_url_hash_A%s_K%s", attrName, transformKey);
        String oldContentHash = mHashStore.getString(contentKey);
        boolean useImgCache = oldContentHash != null && oldContentHash.equals(contentHash);

        String siteBase = null;
        String rewrittenBase = mFlags.getString(ExpFlags.EXP_REWRITTEN_URL);
        if (rewrittenBase.isEmpty()) {
            rewrittenBase = null;
        } else {
            siteBase = mFlags.getString(ExpFlags.EXP_SITE_BASE_URL);
        }

        for (Element element : root.getElementsByAttribute(attrName)) {
            String value = element.attr(attrName);
            String newValue = null;

            URI uri = new URI(value);
            String host = uri.getHost();
            String path = uri.getPath();

            if (host == null || path == null) {
                // This is typically the case with anchor references (e.g. <a href="#chapter">).
                continue;
            }

            Map<String, String> queries = parseQuery(uri);

            if (host.equals("www.google.com") && path.equals("/url")) {
                // Bypass google URL redirector.
                String q = queries.get(QUERY_Q);
                if (q != null && !q.isEmpty()) {
                    newValue = q;

                    if (rewrittenBase != null && newValue.startsWith(rewrittenBase)) {
                        newValue = siteBase + newValue.substring(rewrittenBase.length());
                    }
                }
            } else if (host.equals("docs.google.com") && path.equals("/drawings/image")) {
                // Old style of drawing URLs.
                String id = queries.get("id");
                int w = Integer.parseInt(queries.get(QUERY_W));
                int h = Integer.parseInt(queries.get(QEURY_H));
                newValue = callback.processDrawing(id, w, h, useImgCache);

            } else if (host.equals("docs.google.com") && path.startsWith("/drawings/d/") && path.endsWith("/image")) {
                // Current style of drawing URLs.
                try {
                    String id = path.substring("/drawings/d/".length());
                    id = id.substring(0, id.length() - "/image".length());
                    int w = Integer.parseInt(queries.get(QUERY_W));
                    int h = Integer.parseInt(queries.get(QEURY_H));
                    newValue = callback.processDrawing(id, w, h, useImgCache);
                } catch (Throwable t) {
                    throw new TransformerException("ERROR processing URI " + value
                            + ", Error: " + t);
                }

            } else if (host.contains(".google.com")) {
                // Whatever this is, we should probably do something with it.
                throw new TransformerException("ERROR Unprocessed URL for " + host + ", Path: " + path);

            } else if (attrName.equals(ATTR_SRC) && element.tagName().equals(ELEM_IMG)) {
                CssStyles styles = new CssStyles(element.attr(ATTR_STYLE));
                String sw = element.attr(ATTR_WIDTH);
                String sh = element.attr(ATTR_HEIGHT);
                if (sw.isEmpty()) {
                    sw = styles.get(ATTR_WIDTH);
                }
                if (sh.isEmpty()) {
                    sh = styles.get(ATTR_HEIGHT);
                }
                int w = getIntValue(sw, 0);
                int h = getIntValue(sh, 0);

                newValue = callback.processImage(uri, w, h, useImgCache);
            }

            if (newValue != null) {
                element.attr(attrName, newValue);
            }
        }

        if (!useImgCache) {
            mHashStore.putString(contentKey, contentHash);
        }
    }

    /**
     * Expand youtube embedded URLs into a youtube iframe viewer.
     * This only does it if the URL contains a "&rig4embed" attribute.
     */
    private void rewriteYoutubeEmbed(Element root) throws URISyntaxException {
        for (Element element : root.getElementsByTag(ELEM_A)) {
            String href = element.attr(ATTR_HREF);
            if (!href.contains(QUERY_RIG4EMBED)) {
                continue;
            }
            URI uri = new URI(href);
            boolean isWatch = false;
            boolean isPlaylist = false;
            String id = "";
            Map<String, String> queries = parseQuery(uri);

            if (uri.getHost().equals("www.youtube.com")) {
                isWatch = uri.getPath().equals("/watch");
                isPlaylist = uri.getPath().equals("/playlist");
                if (isWatch) {
                    id = queries.get("v");
                } else if (isPlaylist) {
                    id = queries.get("list");
                }
            }
            if (isWatch || isPlaylist) {
                String width = queries.get(ATTR_WIDTH);
                String height = queries.get(ATTR_HEIGHT);
                String frameborder = queries.get("frameborder");
                String src = "";
                if (isWatch) {
                    src = "https://www.youtube.com/embed/" + id;
                } else if (isPlaylist) {
                    src = "https://www.youtube.com/embed/videoseries?list=" + id;
                }

                if (width == null || width.isEmpty() ){
                    width = "560";
                }
                if (height == null || height.isEmpty()) {
                    height = "315";
                }
                if (frameborder == null || frameborder.isEmpty()) {
                    frameborder = "0";
                }

                Element iframe = new Element(ELEM_IFRAME);
                iframe.attr(ATTR_WIDTH, width);
                iframe.attr(ATTR_HEIGHT, height);
                iframe.attr(ATTR_SRC, src);
                iframe.attr("frameborder", frameborder);
                iframe.attr("allowfullscreen", true);

                element.replaceWith(iframe);
            }
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new TreeMap<>();
        for (NameValuePair pair : URLEncodedUtils.parse(uri, Charsets.UTF_8.name())) {
            map.put(pair.getName(), pair.getValue());
        }
        return map;
    }

    private static int getIntValue(String value, int missingValue) {
        int i = missingValue;
        if (value != null) {
            try {
                i = NumberFormat.getIntegerInstance().parse(value.trim()).intValue();
            } catch (ParseException ignore) {}
        }
        return i;
    }

    public interface Callback {
        /**
         * Process a drawing by downloading it, adjusting it to change to the desired size and
         * returns the href for the new document.
         */
        String processDrawing(String id, int width, int height, boolean useCache) throws IOException;

        /**
         * Process an image by downloading it, adjusting it to change to the desired size and
         * returns the src for the new document.
         */
        String processImage(URI uri, int width, int height, boolean useCache) throws IOException;
    }

    public static class TransformerException extends RuntimeException {
        public TransformerException(String s) {
            super(s);
        }
    }

    private static class CssStyles {
        private final TreeMap<String, String> mMap = new TreeMap<>();

        public CssStyles() {}

        public CssStyles(String attrStyle) {
            parseStyle(attrStyle);
        }

        public CssStyles(CssStyles styles) {
            addAll(styles);
        }

        private void addAll(CssStyles styles) {
            mMap.putAll(styles.mMap);
        }

        public void add(String kvStyle) {
            String[] kv = kvStyle.split(":");
            mMap.put(kv[0], kv.length < 2 ? "" : kv[1]);
        }

        public void parseStyle(String attrStyle) {
            if (attrStyle != null && !attrStyle.isEmpty()) {
                for (String s : attrStyle.split(";")) {
                    add(s);
                }
            }
        }

        public RPair<CssStyles, String> deltaChildStyle(String attrStyle) {
            CssStyles deltaChildStyles = null;

            if (attrStyle != null && !attrStyle.isEmpty()) {
                for (String s : attrStyle.split(";")) {
                    String[] kv = s.split(":");

                    boolean same = mMap.containsKey(kv[0]) && mMap.get(kv[0]).equals(kv.length < 2 ? "" : kv[1]);
                    if (!same) {
                        if (deltaChildStyles == null) {
                            deltaChildStyles = new CssStyles();
                        }
                        deltaChildStyles.add(s);
                    }
                }
            }

            if (deltaChildStyles == null) {
                return null;
            } else {
                CssStyles newParentStyles = new CssStyles(this);
                newParentStyles.addAll(deltaChildStyles);

                return RPair.create(newParentStyles, deltaChildStyles.generateStyle());
            }
        }

        public String generateStyle() {
            StringBuilder sb = new StringBuilder();
            boolean semi = false;
            for (Map.Entry<String, String> entry : mMap.entrySet()) {
                if (semi) {
                    sb.append(';');
                }
                sb.append(entry.getKey()).append(':').append(entry.getValue());
                semi = true;
            }
            return sb.toString();
        }

        public void remove(String name) {
            mMap.remove(name);
        }

        public boolean has(String name) {
            return mMap.containsKey(name);
        }

        public String get(String name) {
            return mMap.get(name);
        }

        public int getIntValue(String name, int missingValue) {
            return HtmlTransformer.getIntValue(mMap.get(name), missingValue);
        }

        public void applyTo(Element element) {
            String style = generateStyle();
            if (style.isEmpty()) {
                element.removeAttr(ATTR_STYLE);
            } else {
                element.attr(ATTR_STYLE, style);
            }
        }
    }
}


