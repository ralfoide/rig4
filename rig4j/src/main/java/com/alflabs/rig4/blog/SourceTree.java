package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;

import java.util.Map;
import java.util.TreeMap;

/**
 * The source tree contains a model of the blog input: category & header for each blog
 * and its posts content list.
 */
class SourceTree {
    private final static String TAG = SourceTree.class.getSimpleName();
    private final Map<String, SourceBlog> mBlogs = new TreeMap<>();
    private boolean mModified;

    public boolean isModified() {
        return mModified;
    }

    @NonNull
    public Map<String, SourceBlog> getBlogs() {
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

        SourceBlog blog = mBlogs.get(category);
        if (blog == null) {
            blog = new SourceBlog(category);
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

            blog.setHeaderContent(SourceContent.from(parsedResult.getIntermediaryHeader()));
        }

        for (BlogSourceParser.ParsedSection section : parsedResult.getParsedSections()) {
            SourceBlogPost post = SourceBlogPost.from(category, section);

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
    public SourceBlog createMixedBlog(@NonNull String mixedCategory, @NonNull CatFilter mixedCatFilter) throws BlogSourceParser.ParseException {

        // If there's already a blog matching the mixed category name, clone it and use it as-is.
        // This way we get the header and all the posts already set. Note that all inside elements
        // (posts, header) are duplicated references and not clones by themselves.
        SourceBlog mixed = mBlogs.get(mixedCategory);
        if (mixed != null) {
            mixed = mixed.cloneBlog();
        } else {
            mixed = new SourceBlog(mixedCategory);
        }

        for (SourceBlog blog : mBlogs.values()) {
            if (blog.getCategory().equals(mixedCategory)) {
                continue;
            }

            // Note: right now all posts from a source blog have the same category.
            // LATER each post will be able to override its own category.
            if (!mixedCatFilter.matches(blog.getCategory())) {
                continue;
            }

            for (SourceBlogPost post : blog.getPosts()) {
                mixed.addPost(post);
            }
        }

        return mixed;
    }

}
