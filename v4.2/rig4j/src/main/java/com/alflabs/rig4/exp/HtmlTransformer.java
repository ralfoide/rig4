package com.alflabs.rig4.exp;

import com.alflabs.utils.RPair;
import com.google.common.base.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

public class HtmlTransformer {

    private static final String ELEM_A = "a";
    private static final String ELEM_HR = "hr";
    private static final String ELEM_SPAN = "span";
    private static final String ELEM_IFRAME = "iframe";
    private static final String ELEM_STYLE = "style";

    private static final String ATTR_ID = "id";
    private static final String ATTR_HREF = "href";
    private static final String ATTR_SRC = "src";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_WIDTH = "width";
    private static final String ATTR_HEIGHT = "height";

    private static final String QUERY_Q = "q";
    private static final String QUERY_W = "w";
    private static final String QEURY_H = "h";
    private static final String QUERY_RIG4EMBED = "rig4embed";

    @Inject
    public HtmlTransformer() {
    }

    /**
     * Simplifies a GDoc exported HTML.
     * Returns the <em>Body</em> element only.
     */
    public String simplify(byte[] content, Callback callback) throws IOException, URISyntaxException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            Document doc = Jsoup.parse(bais, null /* charset */, "" /* base uri */);

            doc = cleanup(doc);
            removeEmptyElements(doc, ELEM_A);
            removeEmptyElements(doc, ELEM_SPAN);
            cleanupInlineStyle(doc);
            rewriteUrls(doc, ATTR_HREF, callback);
            rewriteUrls(doc, ATTR_SRC, callback);
            rewriteYoutubeEmbed(doc);
            removeIzuTags(doc);

            doc.outputSettings().prettyPrint(true);
            doc.outputSettings().charset(Charsets.UTF_8);

            // -- for debugging -- return doc.html();
            Element body = doc.select("body").first();
            return body.html();
        }
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
        for (Element element : root.select(name)) {
            if (element.childNodeSize() == 0) {
                element.remove();
            }
        }
    }

    /**
     * Remove all [izu...] references in the text. I want to be able to add these in
     * current gdocs and not show them by mistake in the final output.
     */
    private void removeIzuTags(Element root) {
        for (Element element : root.select(":containsOwn([izu)")) {
            for (int i = 0, n = element.childNodeSize(); i < n; i++) {
                Node node = element.childNode(i);
                if (node instanceof TextNode) {
                    String text = ((TextNode) node).getWholeText();
                    text = text.replaceAll("\\[izu[^\\]]*\\]", "");
                    ((TextNode) node).text(text);
                }
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
        CssStyles eraseStyles = new CssStyles();
        Element p = root.select("p").first();
        String baseStyle = p == null ? null : p.attr(ATTR_STYLE);
        eraseStyles.parseStyle(baseStyle);
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
     * Rewrite URLs:
     * - The gdoc exporter wraps all URLs using the google URL redirectory. Simply refer to the
     *   source directly and bypass the redirector.
     * - Handle drawing exported PNGs links by downloading them and rewriting them locally.
     * - Any untreated google.com link is an error that should be loooked into.
     */
    private void rewriteUrls(Element root, String attrName, Callback callback) throws IOException, URISyntaxException {
        for (Element element : root.select("[" + attrName + "]")) {
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
                }
            } else if (host.equals("docs.google.com") && path.equals("/drawings/image")) {
                // Old style of drawing URLs.
                String id = queries.get("id");
                int w = Integer.parseInt(queries.get(QUERY_W));
                int h = Integer.parseInt(queries.get(QEURY_H));
                newValue = callback.processDrawing(id, w, h);
            } else if (host.equals("docs.google.com") && path.startsWith("/drawings/d/") && path.endsWith("/image")) {
                // Current style of drawing URLs.
                String id = path.substring("/drawings/d/".length());
                id = id.substring(0, id.length() - "/image".length());
                int w = Integer.parseInt(queries.get(QUERY_W));
                int h = Integer.parseInt(queries.get(QEURY_H));
                newValue = callback.processDrawing(id, w, h);
            } else if (host.contains(".google.com")) {
                // Whatever this is, we should probably do something with it.
                throw new TransformerException("ERROR Unprocessed URL for " + host + ", Path: " + path);
            }

            if (newValue != null) {
                element.attr(attrName, newValue);
            }
        }
    }

    /**
     * Expand youtube embedded URLs into a youtube iframe viewer.
     * This only does it if the URL contains a "&rig4embed" attribute.
     */
    private void rewriteYoutubeEmbed(Element root) throws URISyntaxException {
        for (Element element : root.select(ELEM_A)) {
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

    public interface Callback {
        /** Process a drawing by downloading it, adjusting it to change to the desired size and
         * returns the href for the new document. */
        String processDrawing(String id, int width, int height) throws IOException;
    }

    public static class TransformerException extends RuntimeException {
        public TransformerException(String s) {
            super(s);
        }
    }

    private static class CssStyles {
        private final TreeMap<String, String> mMap = new TreeMap<>();

        public CssStyles() {}

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
    }
}


