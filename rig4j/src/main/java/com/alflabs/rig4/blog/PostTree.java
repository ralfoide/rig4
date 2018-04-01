package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The post tree contains a model of files to be generated based on the input source.
 * Each blog is composed of one index file, paged files, and individual post files.
 */
class PostTree {
    private final static String ROOT = "blog";

    private final Map<String, Blog> mBlogs = new TreeMap<>();

    public void add(Blog blog) {
        mBlogs.put(blog.getCategory(), blog);
    }

    public void generate() {
        for (Blog blog : mBlogs.values()) {
            blog.generate();
        }
    }

    public void saveMetadata() {
    }

    public static class Blog {
        private final String mCategory;
        private final SourceTree.Content mBlogHeader;
        private final BlogPage mBlogIndex;
        private final List<BlogPage> mBlogPages = new ArrayList<>();

        public Blog(String category, SourceTree.Content blogHeader) {
            mCategory = category;
            mBlogHeader = blogHeader;
            mBlogIndex = new BlogPage(new File(ROOT, category));
        }

        public String getCategory() {
            return mCategory;
        }

        public BlogPage getBlogIndex() {
            return mBlogIndex;
        }

        public List<BlogPage> getBlogPages() {
            return mBlogPages;
        }

        public SourceTree.Content getBlogHeader() {
            return mBlogHeader;
        }

        public void generate() {
            mBlogIndex.generate();
            for (BlogPage blogPage : mBlogPages) {
                blogPage.generate();
            }
        }
    }

    public static class BlogPage {
        private final FileItem mFileItem = new FileItem();
        private final List<PostShort> mPostShorts = new ArrayList<>();
        private final List<PostExtra> mPostExtras = new ArrayList<>();

        public BlogPage(BlogPage parent, int index) {
            File path = new File(parent.getFileItem().getPath(), String.format("%03d", index));
            mFileItem.setPath(path);
        }

        public BlogPage(File path) {
            mFileItem.setPath(path);
        }

        public FileItem getFileItem() {
            return mFileItem;
        }

        /**
         * Fill the page with the given posts.
         * The input collection should already be ordered as it should be presented on the page.
         */
        public void fillFrom(Collection<SourceTree.BlogPost> sourcePosts) {
            for (SourceTree.BlogPost sourcePost : sourcePosts) {
                SourceTree.Content pageContent = sourcePost.getFullContent();
                SourceTree.Content extraContent = null;
                if (sourcePost.getShortContent() != null) {
                    extraContent = pageContent;
                    pageContent = sourcePost.getShortContent();
                }

                PostExtra postExtra = null;
                if (extraContent != null) {
                    postExtra = new PostExtra(
                            this,
                            sourcePost.getKey(),
                            sourcePost.getDate(),
                            sourcePost.getTitle(),
                            extraContent);
                    mPostExtras.add(postExtra);
                }

                if (pageContent != null) {
                    mPostShorts.add(
                            new PostShort(
                                    sourcePost.getKey(),
                                    sourcePost.getDate(),
                                    sourcePost.getTitle(),
                                    pageContent,
                                    postExtra));
                }
            }

            mPostShorts.sort(Collections.reverseOrder());
        }

        public void generate() {

        }
    }

    public static class PostShort {
        private final String mKey;
        private final LocalDate mDate;
        private final String mTitle;
        private final SourceTree.Content mContent;
        private final PostExtra mPostExtra;

        public PostShort(
                @NonNull String key,
                @NonNull LocalDate date,
                @NonNull String title,
                @NonNull SourceTree.Content content,
                @Null PostExtra postExtra) {
            mKey = key;
            mDate = date;
            mTitle = title;
            mContent = content;
            mPostExtra = postExtra;
        }
    }

    public static class PostExtra {
        private final FileItem mFileItem = new FileItem();
        private final SourceTree.Content mContent;
        private final String mKey;
        private final LocalDate mDate;
        private final String mTitle;

        public PostExtra(
                @NonNull BlogPage parent,
                @NonNull String key,
                @NonNull LocalDate date,
                @NonNull String title,
                @NonNull SourceTree.Content content) {
            mKey = key;
            mDate = date;
            mTitle = title;
            File path = new File(parent.mFileItem.getPath(), key);
            mFileItem.setPath(path);
            mContent = content;
        }
    }

    public static class FileItem {
        private File mPath;

        public File getPath() {
            return mPath;
        }

        public void setPath(File path) {
            mPath = path;
        }
    }

}
