package com.alflabs.rig4.exp;

import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

    @Inject
    public HtmlTransformer() {
    }

    public byte[] simplify(byte[] content, Callback callback) throws IOException, URISyntaxException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            Document doc = Jsoup.parse(bais, null /* charset */, "" /* base uri */);

            doc = cleanup(doc);
            removeEmptyElements(doc, "a");
            removeEmptyElements(doc, "span");
            cleanupInlineStyle(doc);
            rewriteUrls(doc, "href", callback);
            rewriteUrls(doc, "src", callback);
            rewriteYoutubeEmbed(doc);

            doc.outputSettings().prettyPrint(true);
            doc.outputSettings().charset(Charsets.UTF_8);

            content = doc.html().getBytes(Charsets.UTF_8);
        }
        return content;
    }

    private Document cleanup(Document doc) {
        Whitelist relaxed = Whitelist.relaxed();
        relaxed.addTags("style");
        relaxed.addAttributes(":all", "style");
        Cleaner cleaner = new Cleaner(relaxed);
        doc = cleaner.clean(doc);
        return doc;
    }

    private void removeEmptyElements(Element root, String name) {
        for (Element element : root.select(name)) {
            if (element.childNodeSize() == 0) {
                element.remove();
            }
        }
    }

    private void cleanupInlineStyle(Element root) {
        for (Element element : root.select("[style]")) {
            String style = element.attr("style");

            String newStyle = style.replace("font-family:\"Consolas\"", "font-family:monospace");
            newStyle = newStyle.replaceAll("font-family:\"[^\"]+\"", "");

            if (newStyle.trim().isEmpty()) {
                element.removeAttr("style");
            } else if (!newStyle.equals(style)) {
                element.attr("style", newStyle);
            }
        }
    }

    private void rewriteUrls(Element root, String attrName, Callback callback) throws IOException, URISyntaxException {
        for (Element element : root.select("[" + attrName + "]")) {
            String value = element.attr(attrName);
            String newValue = null;

            URI uri = new URI(value);
            String host = uri.getHost();
            String path = uri.getPath();
            Map<String, String> queries = parseQuery(uri);

            if (host.equals("www.google.com") && path.equals("/url")) {
                String q = queries.get("q");
                if (q != null && !q.isEmpty()) {
                    newValue = q;
                }
            } else if (host.equals("docs.google.com") && path.equals("/drawings/image")) {
                String id = queries.get("id");
                int w = Integer.parseInt(queries.get("w"));
                int h = Integer.parseInt(queries.get("h"));
                newValue = callback.processDrawing(id, w, h);
            } else if (host.equals("docs.google.com") && path.startsWith("/drawings/d/") && path.endsWith("/image")) {
                String id = path.substring("/drawings/d/".length());
                id = id.substring(0, id.length() - "/image".length());
                int w = Integer.parseInt(queries.get("w"));
                int h = Integer.parseInt(queries.get("h"));
                newValue = callback.processDrawing(id, w, h);
            } else if (host.equals("docs.google.com")) {
                throw new TransformerException("ERROR Unprocessed URL for " + host + ", Path: " + path);
            }

            if (newValue != null) {
                element.attr(attrName, newValue);
            }
        }
    }

    private void rewriteYoutubeEmbed(Element root) throws URISyntaxException {
        for (Element element : root.select("a")) {
            String href = element.attr("href");
            if (!href.contains("rig4embed")) {
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
                String width = queries.get("width");
                String height = queries.get("height");
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

                Element iframe = new Element("iframe");
                iframe.attr("width", width);
                iframe.attr("height", height);
                iframe.attr("src", src);
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
}
