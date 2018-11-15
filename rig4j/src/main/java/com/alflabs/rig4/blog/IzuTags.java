package com.alflabs.rig4.blog;

public final class IzuTags {
    private IzuTags() {}

    public static final String PREFIX = "izu:";
    public static final String PARAM_SEP = ":";

    // -- Blog Header Tags

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

    // -- Blog Posts Tags

    /** Tag: [izu:break] -- Indicates the break between short/full text in a blog post. */
    public static final String IZU_BREAK = "izu:break";

    /** Tag: [izu:cat:{category-name}] -- In the blog header, indicates the default category for
     * each blog post. In a blog post, overrides the default cateogry. */
    public static final String IZU_CATEGORY = "izu:cat:";

    /** Tag: [izu:link-img:{link}] -- Indicates that the preceding image should use the specified "A HREF" link.
     * If the link is omitted, the previous A HREF found in the document *before* the image will be used. */
    public static final String IZU_LINK_IMG = "izu:link-img:";
}
