package com.alflabs.rig4.blog;

import autovalue.shaded.org.apache.commons.lang.StringEscapeUtils;
import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.EntryPoint;
import com.google.common.base.Charsets;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * ATOM reference: https://tools.ietf.org/html/rfc4287
 */
public class AtomWriter {
    // Number of posts to include in full. All following posts use the short summary.
    private static final int NUM_POST_FULL = 10;
    // Max size of a full post in characters. Switch to short summary above this size.
    private static final int POST_FULL_LENGTH = 100000;

    public void write(@NonNull PostTree.Blog blog,
                      @NonNull BlogGenerator.Generator generator,
                      @NonNull PostTree.FileItem fileItem)
            throws IOException, URISyntaxException {
        File destFile = new File(generator.getDestDir(), fileItem.getLeafFile());
        generator.getFileOps().createParentDirs(destFile);

        StringBuilder generated = new StringBuilder();

        generated.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        generated.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");

        String dateGen = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        tag(generated, "title", blog.getTitle(), "type", "html");
        tag(generated, "updated", dateGen);

        attr(generated, "link",
                "rel", "alternate",
                "type", "text/html",
                "hreflang", "en",
                "href", generator.getAbsSiteLink() + fileItem.getLeafDirWeb());

        attr(generated, "link",
                "rel", "self",
                "type", "application/atom+xml",
                "hreflang", "en",
                "href", generator.getAbsSiteLink() + fileItem.getLeafFile());

        tag(generated, "id", generator.getAbsSiteLink() + fileItem.getLeafFile());

        tag(generated, "generator", "Rig4j",
                "uri", "https://github.com/ralfoide/rig4",
                "version", EntryPoint.getVersion());

        int num = 0;
        Set<String> visited = new HashSet<>();
        for (PostTree.BlogPage blogPage : blog.getBlogPages()) {
            for (PostTree.PostShort postShort : blogPage.getPostShorts()) {
                PostTree.PostFull postFull = postShort.mPostFull;
                if (visited.contains(postFull.mKey)) {
                    continue;
                }
                visited.add(postFull.mKey);

                entry(destFile, generated, blog, generator, postShort, postFull, num >= NUM_POST_FULL);
                num++;
            }
        }

        generated.append("</feed>\n");

        generator.getFileOps().writeBytes(generated.toString().getBytes(Charsets.UTF_8), destFile);
    }

    private void entry(@NonNull File destFile,
                       @NonNull StringBuilder generated,
                       @NonNull PostTree.Blog blog,
                       @NonNull BlogGenerator.Generator generator,
                       @NonNull PostTree.PostShort postShort,
                       @NonNull PostTree.PostFull postFull,
                       boolean usePostShort) throws IOException, URISyntaxException {
        generated.append("<entry>\n");

        generated.append("<author><name>Ralf</name></author>\n"); // TODO make configurable

        tag(generated, "title", postFull.mTitle, "type", "html");
        attr(generated, "link",
                "rel", "alternate",
                "type", "text/html",
                "href", generator.getAbsSiteLink() + postFull.mFileItem.getLeafFile());

        tag(generated, "id", generator.getAbsSiteLink() + postFull.mFileItem.getLeafFile());

        attr(generated, "category",
                "term", postFull.mCategory,
                "label", generator.categoryToHtml(postFull.mCategory));

        // Leaky abstraction: this is needed to setup the content formatter according to the post's HTML dest file.
        String content = null;
        if (!usePostShort) {
            // Generate the full content,
            // then check if we need to switch to the short content if it's too large.
            postFull.prepareHtmlDestFile(blog, generator);
            String contentFull = postFull.mContent.getFormatted();
            if (contentFull.length() <= POST_FULL_LENGTH) {
                content = contentFull;
            }
        }
        if (usePostShort || content == null) {
            postShort.prepareContent(generator, destFile);
            content = postShort.mContent.getFormatted()
                + "<br><p>(Abridged version... follow web site link to read more)";
        }

        tag(generated, "content",
                content,
                "type", "html",     // per https://tools.ietf.org/html/rfc4287#section-4.1.3
                "xml:lang", "en",
                "xml:base", generator.getAbsSiteLink() + postFull.mFileItem.getLeafDirWeb());

        // Right now, post's time is only a Date by design. Generate a time using 0 hour 0 minutes.
        // Set the seconds to the modulo 60 of the hash of the content -- this way an update will
        // potentially generate a slightly different update time. Wacky but good enough here.
        // SHA-1 is enough here as we just use the first byte of the hash.
        byte[] bytes = DigestUtils.sha1(content);
        int seconds = bytes[0] & 0x00FF; // To avoid "negative byte" values due to lack of unsigned types
        seconds = seconds % 60;
        String updated = postFull.mDate
                .atTime(0, 0, seconds)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        tag(generated, "updated", updated);

        generated.append("</entry>\n");
    }

    /**
     * Generates one {@code <tag>content</tag>} line.
     * Content is XML escaped.
     */
    public static void tag(@NonNull StringBuilder generated,
                           @NonNull String tag,
                           @NonNull String content) {
        tag(generated, tag, content, (String[]) null);
    }

    /**
     * Generates one {@code <tag attr1="value1" attr2="value2"... />} line, with no content.
     * Values are XML escaped.
     */
    public static void attr(@NonNull StringBuilder generated,
                            @NonNull String tag,
                            @NonNull String...attrValues) {
        tag(generated, tag, null, attrValues);
    }

    /**
     * Generates one {@code <tag attr1="value1" attr2="value2">content</tag>} line.
     * Content is optional if null. Attributes are optional if null.
     * Values & content are XML escaped.
     */
    public static void tag(@NonNull StringBuilder generated,
                            @NonNull String tag,
                            @Null String content,
                            @Null String...attrValues) {
        generated.append('<').append(tag);

        if (attrValues != null && attrValues.length > 0) {
            for (int i = 0; i < attrValues.length; i += 2) {
                generated.append(' ')
                        .append(attrValues[i])
                        .append("=\"")
                        .append(StringEscapeUtils.escapeHtml(attrValues[i + 1]))
                        .append('"');
            }
        }
        if (content != null && !content.isEmpty()) {
            generated.append('>');
            generated.append(StringEscapeUtils.escapeXml(content));
            generated.append("</").append(tag).append(">\n");
        } else {
            generated.append("/>\n");
        }
    }
}
