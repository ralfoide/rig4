package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class IzuTags {
    private IzuTags() {}

    public static final String PREFIX = "izu:";
    public static final String PARAM_SEP = ":";

    // -- SourceBlog Header Tags

    /** Tag: [izu:blog] -- Indicates this a blog. */
    public static final String IZU_BLOG = "izu:blog";

    /** Tag: [izu:blog-tile:{title with spaces}]. -- Overrides the default title of a blog. */
    public static final String IZU_BLOG_TITLE = "izu:blog-title:";

    /** Tag: [izu:blog-accept-cat] -- A regexp list of categories to accept from sources. */
    public static final String IZU_BLOG_ACCEPT_CAT = "izu:" + BlogFlags.BLOG_ACCEPT_CAT;
    /** Tag: [izu:blog-reject-cat] -- A regexp list of categories to reject from sources. */
    public static final String IZU_BLOG_REJECT_CAT = "izu:" + BlogFlags.BLOG_REJECT_CAT;
    /** Tag: [izu:blog-gen-single] -- A regexp list of categories for which to generate single category pages. */
    public static final String IZU_BLOG_GEN_SINGLE = "izu:" + BlogFlags.BLOG_GEN_SINGLE;
    /** Tag: [izu:blog-banner-exclude] -- A regexp list of categories for which to generate banner links. */
    public static final String IZU_BLOG_BANNER_EXCLUDE = "izu:" + BlogFlags.BLOG_BANNER_EXCLUDE;
    /** Tag: [izu:blog-gen-mixed] -- A regexp list of categories to include in the mixed-categories pages. */
    public static final String IZU_BLOG_GEN_MIXED = "izu:" + BlogFlags.BLOG_GEN_MIXED;
    /** Tag: [izu:blog-mixed-cat] -- Synthetic category name used for the mixed-categories pages. */
    public static final String IZU_BLOG_MIXED_CAT = "izu:" + BlogFlags.BLOG_MIXED_CAT;

    /** Tag: [izu:header:end] -- Indicates the end of the static header of a blog file. */
    public static final String IZU_HEADER_END = "izu:header:end";

    /** Tag: [izu:blog:end] -- Indicates this is the end of the parsed section of a blog file. */
    public static final String IZU_BLOG_END = "izu:blog:end";

    // -- SourceBlog Posts Tags

    /** Tag: [izu:break] -- Indicates the break between short/full text in a blog post. */
    public static final String IZU_BREAK = "izu:break";

    /** Tag: [izu:cat:{category-name}] -- In the blog header, indicates the default category for
     * each blog post. In a blog post, overrides the default cateogry. */
    public static final String IZU_CATEGORY = "izu:cat:";

    /** Tag: [izu:link-img:{link}] -- Indicates that the preceding image should use the specified "A HREF" link.
     * If the link is omitted, the previous A HREF found in the document *before* the image will be used. */
    public static final String IZU_LINK_IMG = "izu:link-img:";

    /** Tag: [izu:desc:{long description}] -- In the blog header, post, or page, indicates the
     * summary description to place in the generated page metadata.
     * Inferred from content when not available. */
    public static final String IZU_DESC = "izu:desc:";

    /** Tag: [izu:old_s:{date:old title}] -- In a blog post, indicates the older titles of
     * that blog that must be used to create redirector full blog pages. */
    public static final String IZU_OLD_S = "izu:old_s:";


    /**
     * Indicates if there's a tag starting with that prefix in that tag list/collection.
     *
     * @param izuTagPrefix A tag prefix ending with ":" such as {@link #IZU_BLOG_TITLE} or {@link #IZU_DESC}.
     * @param tags A list of tags, possibly empty.
     * @return True if there's at least one tag starting with that prefix, whether it has a value or not.
     */
    public static boolean hasPrefixTag(@NonNull String izuTagPrefix, @NonNull Collection<String> tags) {
        for (String izuTag : tags) {
            if (izuTag.startsWith(izuTagPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the value of an izu tag in a tag list/collection.
     * If multiple occurrences of the tag are in the list, returns the first non-empty one.
     *
     * @param izuTagPrefix A tag prefix ending with ":" such as {@link #IZU_BLOG_TITLE} or {@link #IZU_DESC}.
     * @param tags A list of tags, possibly empty.
     * @return A non-null value. The empty string is returned if nothing is found for easier chaining.
     *  If non empty, the string is already trimmed.
     */
    @NonNull
    public static String getTagValue(@NonNull String izuTagPrefix, @NonNull Collection<String> tags) {
        for (String izuTag : tags) {
            if (izuTag.startsWith(izuTagPrefix)) {
                String title = izuTag.substring(izuTagPrefix.length()).trim();
                if (!title.isEmpty()) {
                    return title;
                }
            }
        }
        return "";
    }

    /**
     * Extracts the values of an izu tag in a tag list/collection.
     * If multiple occurrences of the tag are in the list, returns the them all in the same order.
     *
     * @param izuTagPrefix A tag prefix ending with ":" such as {@link #IZU_BLOG_TITLE} or {@link #IZU_DESC}.
     * @param tags A list of tags, possibly empty.
     * @return A non-null, possibly empty, list of valuestrings, each already trimmed.
     */
    @NonNull
    public static List<String> getTagValues(@NonNull String izuTagPrefix, @NonNull Collection<String> tags) {
        ArrayList<String> results = new ArrayList<>();
        for (String izuTag : tags) {
            if (izuTag.startsWith(izuTagPrefix)) {
                String title = izuTag.substring(izuTagPrefix.length()).trim();
                if (!title.isEmpty()) {
                    results.add(title);
                }
            }
        }
        return results;
    }
}
