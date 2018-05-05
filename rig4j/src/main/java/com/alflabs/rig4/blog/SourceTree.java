package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.HtmlTransformer;
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
class SourceTree extends TreeChange {
    private Map<String, Blog> mBlogs = new TreeMap<>();

    public Map<String, Blog> getBlogs() {
        return mBlogs;
    }

    public void saveMetadata() {
    }

    public void merge(BlogSourceParser.ParsedResult parsedResult, boolean fileChanged)
            throws BlogSourceParser.ParseException {
        String category = parsedResult.getBlogCategory();
        Blog blog = mBlogs.get(category);
        if (blog == null) {
            blog = new Blog(category);
            mBlogs.put(category, blog);
        }
        // LATER for now invalidate the whole blog.
        blog.setChanged(blog.isChanged() || fileChanged);

        if (parsedResult.getIntermediaryHeader() != null) {
            if (blog.getHeaderContent() != null) {
                throw new BlogSourceParser.ParseException("Duplicate blog headers defined for category "
                    + category + ". Only one can be defined. Please adjust the " + IzuTags.IZU_HEADER_END
                    + "accordingly. Tip: " + IzuTags.IZU_BLOG + " doesn't have to be on the first line.");
            }

            blog.setHeaderContent(Content.from(parsedResult.getIntermediaryHeader()));
        }

        for (BlogSourceParser.ParsedSection section : parsedResult.getParsedSections()) {
            BlogPost post = BlogPost.from(section);
            // LATER post.setChanged(...)
            blog.addPost(post);
        }
    }

    @Override
    public boolean isTreeChanged() {
        if (isChanged()) {
            return true;
        }
        for (Blog blog : mBlogs.values()) {
            if (blog.isChanged()) {
                return true;
            }
        }
        return false;
    }

    public static class Blog extends TreeChange {
        private final String mCategory;
        private Content mHeaderContent;
        private Map<String, BlogPost> mPosts = new TreeMap<>();

        public Blog(String category) {
            mCategory = category;
        }

        public String getCategory() {
            return mCategory;
        }

        public Collection<BlogPost> getPosts() {
            return mPosts.values();
        }

        public Content getHeaderContent() {
            return mHeaderContent;
        }

        public void setHeaderContent(Content headerContent) {
            mHeaderContent = headerContent;
        }

        public void addPost(BlogPost post) throws BlogSourceParser.ParseException {
            String key = post.getKey();
            if (mPosts.containsKey(key)) {
                throw new BlogSourceParser.ParseException(
                        "Duplicate post key '" + key + "' in blog category " + mCategory);
            }
            mPosts.put(key, post);
        }

        @Override
        public boolean isTreeChanged() {
            if (isChanged()) {
                return true;
            }
            for (BlogPost post : mPosts.values()) {
                if (post.isChanged()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class BlogPost extends TreeChange implements Comparable<BlogPost> {
        private final LocalDate mDate;
        private final String mTitle;
        private final Content mShortContent;
        private final Content mFullContent;
        private final String mKey;

        public BlogPost(
                @NonNull LocalDate date,
                @NonNull String title,
                @Null Content shortContent,
                @NonNull Content fullContent) {
            mDate = date;
            mTitle = title;
            mShortContent = shortContent;
            mFullContent = fullContent;

            mKey = mDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    + "_"
                    + title.toLowerCase(Locale.US)
                        .replaceAll("[^a-z0-9_-]", "_")
                        .replaceAll("_+", "_");
        }

        public String getKey() {
            return mKey;
        }

        public LocalDate getDate() {
            return mDate;
        }

        public String getTitle() {
            return mTitle;
        }

        public Content getShortContent() {
            return mShortContent;
        }

        public Content getFullContent() {
            return mFullContent;
        }

        @NonNull
        public static BlogPost from(@NonNull BlogSourceParser.ParsedSection section) {
            return new BlogPost(
                    section.getDate(),
                    section.getTextTitle(),
                    Content.from(section.getIntermediaryShort()),
                    Content.from(section.getIntermediaryfull()));
        }

        @Override
        public int compareTo(BlogPost other) {
            return this.mKey.compareTo(other.mKey);
        }

        @Override
        public boolean isTreeChanged() {
            return isChanged();
        }
    }

    public static class Content {
        private final Element mIntermediary;
        private String mFormatted;
        private HtmlTransformer.LazyTransformer mTransformer;

        public Content(@Null String formatted, @Null Element intermediary) {
            mFormatted = formatted;
            mIntermediary = intermediary;
        }

        public void setTransformer(@NonNull HtmlTransformer.LazyTransformer transformer) {
            mTransformer = transformer;
        }

        @Null
        public String getFormatted() throws IOException, URISyntaxException {
            if (mFormatted == null && mIntermediary != null) {
                mFormatted = mTransformer.lazyTransform(mIntermediary);
            }
            return mFormatted;
        }

        @Null
        public Element getIntermediary() {
            return mIntermediary;
        }

        @Null
        public static Content from(@Null Element intermediary) {
            return intermediary == null ? null : new Content(null, intermediary);
        }
    }

}
