package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.HtmlTransformer;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The source tree contains a model of the blog input: category & header for each blog
 * and its posts content list.
 */
class SourceTree {
    private final static String TAG = SourceTree.class.getSimpleName();
    private Map<String, Blog> mBlogs = new TreeMap<>();
    private boolean mModified;

    public boolean isModified() {
        return mModified;
    }

    @NonNull
    public Map<String, Blog> getBlogs() {
        return mBlogs;
    }

    public void saveMetadata() {
    }

    public void merge(@NonNull BlogSourceParser.ParsedResult parsedResult,
                      boolean fileChanged,
                      @NonNull CatFilter catAcceptFilter,
                      @NonNull CatFilter catRejectFilter)
            throws BlogSourceParser.ParseException {
        mModified |= fileChanged;
        String category = parsedResult.getBlogCategory();

        if (!catAcceptFilter.matches(category)) {
            return;
        }
        if (catRejectFilter.matches(category)) {
            return;
        }

        Blog blog = mBlogs.get(category);
        if (blog == null) {
            blog = new Blog(category);
            mBlogs.put(category, blog);
        }

        // TODO LATER for now invalidate the whole blog.
        // blog.setChanged(blog.isChanged() || fileChanged);

        if (blog.getTitle() == null) {
            String title = IzuTags.getTagValue(IzuTags.IZU_BLOG_TITLE, parsedResult.getTags());
            if (!title.isEmpty()) {
                blog.setTitle(title);
            }
        }

        if (parsedResult.getIntermediaryHeader() != null) {
            if (blog.getHeaderContent() != null) {
                throw new BlogSourceParser.ParseException("Duplicate blog headers defined for category "
                    + category + ". Only one can be defined. Please adjust the " + IzuTags.IZU_HEADER_END
                    + "accordingly. Tip: " + IzuTags.IZU_BLOG + " doesn't have to be on the first line "
                    + "and everything before this tag in the file is ignored.");
            }

            blog.setHeaderContent(Content.from(parsedResult.getIntermediaryHeader()));
        }

        for (BlogSourceParser.ParsedSection section : parsedResult.getParsedSections()) {
            BlogPost post = BlogPost.from(category, section);

            if (!catAcceptFilter.matches(post.getCategory())) {
                continue;
            }
            if (catRejectFilter.matches(post.getCategory())) {
                continue;
            }

            // TODO LATER post.setChanged(...) on a per-post basis
            blog.addPost(post);
        }
    }

    /**
     * Creates a new synthetic source blog merging all the posts from blogs matching the
     * mixed cat filter.
     */
    @NonNull
    public Blog createMixedBlog(@NonNull String mixedCategory, @NonNull CatFilter mixedCatFilter) throws BlogSourceParser.ParseException {

        // If there's already a blog matching the mixed category name, clone it and use it as-is.
        // This way we get the header and all the posts already set. Note that all inside elements
        // (posts, header) are duplicated references and not clones by themselves.
        Blog mixed = mBlogs.get(mixedCategory);
        if (mixed != null) {
            mixed = mixed.cloneBlog();
        } else {
            mixed = new Blog(mixedCategory);
        }

        for (Blog blog : mBlogs.values()) {
            if (blog.getCategory().equals(mixedCategory)) {
                continue;
            }

            // Note: right now all posts from a source blog have the same category.
            // LATER each post will be able to override its own category.
            if (!mixedCatFilter.matches(blog.getCategory())) {
                continue;
            }

            for (BlogPost post : blog.getPosts()) {
                mixed.addPost(post);
            }
        }

        return mixed;
    }

    public static class Blog {
        private final String mCategory;
        private String mTitle;
        private Content mHeaderContent;
        private Map<String, BlogPost> mPosts = new TreeMap<>();

        public Blog(@NonNull String category) {
            mCategory = category;
        }

        /**
         * Creates a shallow clone of this blog.
         * Note that all inside elements (posts, header) are duplicated references
         * and not clones by themselves.
         */
        @NonNull
        public Blog cloneBlog() {
            Blog copy = new Blog(mCategory);
            copy.mTitle = mTitle;
            copy.mHeaderContent = mHeaderContent;
            copy.mPosts.putAll(mPosts);
            return copy;
        }

        @NonNull
        public String getCategory() {
            return mCategory;
        }

        @NonNull
        public Collection<BlogPost> getPosts() {
            return mPosts.values();
        }

        @Null
        public String getTitle() {
            return mTitle;
        }

        public void setTitle(@NonNull String title) {
            mTitle = title;
        }

        @Null
        public Content getHeaderContent() {
            return mHeaderContent;
        }

        public void setHeaderContent(@NonNull Content headerContent) {
            mHeaderContent = headerContent;
        }

        public void addPost(@NonNull BlogPost post) throws BlogSourceParser.ParseException {
            String key = post.getKey();
            if (mPosts.containsKey(key)) {
                throw new BlogSourceParser.ParseException(
                        "Duplicate post key '" + key + "' in blog category " + mCategory);
            }
            mPosts.put(key, post);
        }
    }

    public static class BlogPost implements Comparable<BlogPost> {
        private final String mCategory;
        private final LocalDate mDate;
        private final String mTitle;
        private final Content mShortContent;
        private final Content mFullContent;
        private final String mKey;

        public BlogPost(
                @NonNull String category,
                @NonNull LocalDate date,
                @NonNull String title,
                @Null Content shortContent,
                @NonNull Content fullContent) {
            mCategory = category;
            mDate = date;
            mTitle = title;
            mShortContent = shortContent;
            mFullContent = fullContent;

            String key = mDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    + "_"
                    + title.trim().toLowerCase(Locale.US)
                        .replaceAll("[^a-z0-9_-]", "_")
                        .replaceAll("_+", "_");

            final int maxKeyLen = 48;
            final int maxShaLen =  8;
            if (key.length() > maxKeyLen) {
                String shaHex = DigestUtils.sha1Hex(key);
                key = key.substring(0, maxKeyLen - 1 - maxShaLen) + "_" + shaHex.substring(0, maxShaLen);
            }

            mKey = key;
        }

        public String getCategory() {
            return mCategory;
        }

        @NonNull
        public String getKey() {
            return mKey;
        }

        @NonNull
        public LocalDate getDate() {
            return mDate;
        }

        @NonNull
        public String getTitle() {
            return mTitle;
        }

        @Null
        public Content getShortContent() {
            return mShortContent;
        }

        @NonNull
        public Content getFullContent() {
            return mFullContent;
        }

        @NonNull
        public static BlogPost from(@NonNull String mainBlogCategory,
                                    @NonNull BlogSourceParser.ParsedSection section)
                throws BlogSourceParser.ParseException {

            String postCategory = IzuTags.getTagValue(IzuTags.IZU_CATEGORY, section.getIzuTags());
            if (postCategory.isEmpty() && IzuTags.hasPrefixTag(IzuTags.IZU_CATEGORY, section.getIzuTags())) {
                throw new BlogSourceParser.ParseException(
                        String.format("Invalid tag '%s' in section [%s:%s]",
                                IzuTags.IZU_CATEGORY,
                                section.getDate(),
                                section.getTextTitle()));
            }

            return new BlogPost(
                    postCategory.isEmpty() ? mainBlogCategory : postCategory,
                    section.getDate(),
                    section.getTextTitle(),
                    Content.from(section.getIntermediaryShort()),
                    Content.from(section.getIntermediaryfull()));
        }

        @Override
        public int compareTo(BlogPost other) {
            return this.mKey.compareTo(other.mKey);
        }
    }

    /**
     * Intermediary HTML content that has been partially provided by the HtmlTransformer yet
     * is not fully formatted, as the formatting depends on the context where the content
     * is going to be used (e.g. to generate image assets in the right directories).
     */
    public static class Content {
        private final Element mIntermediary;
        private Element mFormatted;
        private String mTransformerKey;
        private HtmlTransformer.LazyTransformer mTransformer;

        /**
         * Creates an empty content.
         * <p/>
         * Use {@link #from(Element)} to create content from an existing {@link Element} node.
         */
        public Content() {
            mFormatted = null;
            mIntermediary = null;
        }

        /**
         * Creates content with either fully formatted content or intermediary content yet to be formatted.
         * One or the other should be provided.
         */
        private Content(/*@Null String formatted,*/ @Null Element intermediary) {
            mFormatted = null;
            mIntermediary = intermediary;
        }

        /**
         * Transformer needed to transform the intermediary content into formatted content.
         */
        public void setTransformer(@NonNull HtmlTransformer.LazyTransformer transformer) {
            mTransformer = transformer;
            String newKey = transformer.getTransformKey();
            if (mFormatted != null && (mTransformerKey == null || !mTransformerKey.equals(newKey))) {
                mFormatted = null;
            }
            mTransformerKey = newKey;
        }

        /**
         * Returns the formatted content.
         * If a transformer is available, intermediary content is transformed first.
         * Each transformer has a key; when a new transformer is set, the content is regenerated
         * if the key does not match.
         */
        @Null
        public String getFormatted() throws IOException, URISyntaxException {
            if (mFormatted == null && mIntermediary != null) {
                Preconditions.checkNotNull(mTransformer);
                mFormatted = mTransformer.lazyTransform(mIntermediary);
            }
            return mFormatted == null ? null : mFormatted.html();
        }

        /**
         * Extracts the first "img src" attribute found in the formatted content.
         * {@link #getFormatted()} must have been called first to generate the formatted content.
         */
        @Null
        public String getFormattedFirstImageSrc() {
            if (mFormatted != null) {
                Preconditions.checkNotNull(mTransformer);
                return mTransformer.getFormattedFirstImageSrc(mFormatted);
            }
            return null;
        }

        /**
         * Extracts the first paragraph description found in the formatted content.
         * {@link #getFormatted()} must have been called first to generate the formatted content.
         */
        @Null
        public String getFormattedDescription() {
            if (mFormatted != null) {
                Preconditions.checkNotNull(mTransformer);
                return mTransformer.getFormattedDescription(mFormatted);
            }
            return null;
        }

        @Null
        public Element getIntermediary() {
            return mIntermediary;
        }

        @Null
        public static Content from(@Null Element intermediary) {
            return intermediary == null ? null : new Content(intermediary);
        }
    }

}
