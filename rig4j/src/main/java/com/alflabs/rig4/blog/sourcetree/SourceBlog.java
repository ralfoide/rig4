package com.alflabs.rig4.blog.sourcetree;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.blog.BlogSourceParser;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class SourceBlog {
    private final String mCategory;
    private String mTitle;
    private SourceContent mHeaderContent;
    private final SortedMap<String, SourceBlogPost> mPosts = new TreeMap<>();

    public SourceBlog(@NonNull String category) {
        mCategory = category;
    }

    /**
     * Creates a shallow clone of this blog.
     * Note that all inside elements (posts, header) are duplicated references
     * and not clones by themselves.
     */
    @NonNull
    public SourceBlog cloneBlog() {
        SourceBlog copy = new SourceBlog(mCategory);
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
    public Collection<SourceBlogPost> getPosts() {
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
    public SourceContent getHeaderContent() {
        return mHeaderContent;
    }

    public void setHeaderContent(@NonNull SourceContent headerContent) {
        mHeaderContent = headerContent;
    }

    public void addPost(@NonNull SourceBlogPost post) throws BlogSourceParser.ParseException {
        String key = post.getKey();
        if (mPosts.containsKey(key)) {
            throw new BlogSourceParser.ParseException(
                    "Duplicate post key '" + key + "' in blog category " + mCategory);
        }
        mPosts.put(key, post);
    }
}
