package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.Templater;
import com.google.common.base.Charsets;

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
    private final static String TAG = PostTree.class.getSimpleName();
    private final static String ROOT = "blog";
    private static final String HTML = ".html";
    private static final String INDEX_HTML = "index.html";

    private final Map<String, Blog> mBlogs = new TreeMap<>();

    public void add(@NonNull Blog blog) {
        mBlogs.put(blog.getCategory(), blog);
    }

    @Null
    public Blog get(@NonNull String category) {
        return mBlogs.get(category);
    }

    public void generate(@NonNull BlogGenerator.Generator generator) throws Exception {
        for (Blog blog : mBlogs.values()) {
            blog.generate(generator);
        }
    }

    public void saveMetadata() {
    }

    public static class Blog {
        private final String mCategory;
        private final String mTitle;
        private final SourceTree.Content mBlogHeader;
        /** mBlogIndex is page in mBlogPages. */
        private final BlogPage mBlogIndex;
        private final List<BlogPage> mBlogPages = new ArrayList<>();

        public Blog(@NonNull String category, @Null String title, @Null SourceTree.Content blogHeader) {
            mCategory = category;
            mTitle = title == null ? "" : title;
            mBlogHeader = blogHeader != null ? blogHeader : new SourceTree.Content("", null);
            mBlogIndex = new BlogPage(this, new File(ROOT, category));
            mBlogPages.add(mBlogIndex);
        }

        @NonNull
        public String getCategory() {
            return mCategory;
        }

        @NonNull
        public String getTitle() {
            return mTitle;
        }

        @NonNull
        public BlogPage getBlogIndex() {
            return mBlogIndex;
        }

        @NonNull
        public List<BlogPage> getBlogPages() {
            return mBlogPages;
        }

        @NonNull
        public SourceTree.Content getBlogHeader() {
            return mBlogHeader;
        }

        public void generate(@NonNull BlogGenerator.Generator generator) throws Exception {
            for (int i = 0; i < mBlogPages.size(); i++) {
                mBlogPages.get(i).generate(i, mBlogPages, generator);
            }
        }
    }

    public static class BlogPage {
        private final Blog mBlog;
        private final FileItem mFileItem;
        private final List<PostShort> mPostShorts = new ArrayList<>();
        private final List<PostExtra> mPostExtras = new ArrayList<>();

        /** Create a top-level index page. */
        public BlogPage(@NonNull Blog blog, @NonNull File dir) {
            mBlog = blog;
            mFileItem = new FileItem(dir, INDEX_HTML);
        }

        /** Create a numbered index page. */
        public BlogPage(@NonNull Blog blog, @NonNull BlogPage parent, int index) {
            mBlog = blog;
            mFileItem = new FileItem(parent.getFileItem().getDir(), String.format("%04x", index) + HTML);
        }

        @NonNull
        public FileItem getFileItem() {
            return mFileItem;
        }

        /**
         * Fill the page with the given posts.
         * The input collection should already be ordered as it should be presented on the page.
         */
        public void fillFrom(@NonNull Collection<SourceTree.BlogPost> sourcePosts) {
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
                            sourcePost.getCategory(),
                            sourcePost.getKey(),
                            sourcePost.getDate(),
                            sourcePost.getTitle(),
                            extraContent);
                    mPostExtras.add(postExtra);
                }

                if (pageContent != null) {
                    mPostShorts.add(
                            new PostShort(
                                    sourcePost.getCategory(),
                                    sourcePost.getKey(),
                                    sourcePost.getDate(),
                                    sourcePost.getTitle(),
                                    pageContent,
                                    postExtra));
                }
            }

            mPostShorts.sort(Collections.reverseOrder());
        }

        public void generate(int index,
                             @NonNull List<BlogPage> blogPages,
                             @NonNull BlogGenerator.Generator generator) throws Exception {
            // Write one file with all the short entries.
            generateMainPage(index, blogPages, generator);

            // Write one file per extra entry.
            for (PostExtra postExtra : mPostExtras) {
                generateExtraPage(generator, postExtra);
            }
        }

        private void generateMainPage(int index,
                                      @NonNull List<BlogPage> blogPages,
                                      @NonNull BlogGenerator.Generator generator) throws Exception {
            File destFile = new File(generator.getDestDir(), mFileItem.getLeafFile());

            generator.getFileOps().createParentDirs(destFile);
            generator.getLogger().d(TAG, "Generate page for blog: " + mBlog.getCategory()
                    + ", file: " + destFile);

            StringBuilder content = new StringBuilder();
            for (PostShort postShort : mPostShorts) {
                content.append(generateShort(generator, destFile, postShort));
            }

            mBlog.getBlogHeader().setTransformer(generator.getLazyHtmlTransformer(destFile));

            String prevPageLink = null;
            String nextPageLink = null;
            if (index > 0) {
                prevPageLink = blogPages.get(index - 1).mFileItem.getName();
            }
            if (index < blogPages.size() - 1) {
                nextPageLink = blogPages.get(index + 1).mFileItem.getName();
            }

            Templater.BlogPageData templateData = Templater.BlogPageData.create(
                    generator.getSiteTitle(),
                    generator.getSiteBaseUrl(),
                    generator.getSiteBanner(),
                    generator.getSiteCss(),
                    generator.getGAUid(),
                    mBlog.getTitle(),
                    destFile.getName(), // page filename (for base-url/page-filename.html)
                    prevPageLink,
                    nextPageLink,
                    mBlog.getBlogHeader().getFormatted(),
                    "",                 // no post title for an index
                    "",                 // no post date  for an index
                    "",                 // no post category for an index
                    "",                 // no post cat link for an index
                    content.toString()


            );

            String generated = generator.getTemplater().generate(templateData);
            generator.getFileOps().writeBytes(generated.getBytes(Charsets.UTF_8), destFile);
        }

        private String generateShort(
                @NonNull BlogGenerator.Generator generator,
                @NonNull File destFile,
                @NonNull PostShort postData)
                throws Exception {
            generator.getLogger().d(TAG, "Generate short: " + postData.mKey
                    + ", title: '" + postData.mTitle + "'");

            postData.mContent.setTransformer(generator.getLazyHtmlTransformer(destFile));

            String extraLink = postData.mPostExtra == null ? null : postData.mPostExtra.mFileItem.getName();

            Templater.BlogPostData templateData = Templater.BlogPostData.create(
                    generator.getSiteBaseUrl(),
                    postData.mTitle,
                    postData.mDate.toString(),
                    generator.categoryToHtml(postData.mCategory),
                    generator.linkForCategory(postData.mCategory),
                    extraLink,
                    postData.mContent.getFormatted()
            );

            return generator.getTemplater().generate(templateData);
        }

        private void generateExtraPage(
                @NonNull BlogGenerator.Generator generator,
                @NonNull PostExtra postData)
                throws Exception {
            File destFile = new File(generator.getDestDir(), postData.mFileItem.getLeafFile());
            generator.getFileOps().createParentDirs(destFile);

            postData.mContent.setTransformer(generator.getLazyHtmlTransformer(destFile));
            mBlog.getBlogHeader().setTransformer(generator.getLazyHtmlTransformer(destFile));

            generator.getLogger().d(TAG, "Generate extra: " + postData.mKey + " (" + postData.mTitle + ")"
                    + ", file: " + destFile);

            Templater.BlogPageData templateData = Templater.BlogPageData.create(
                    generator.getSiteTitle(),
                    generator.getSiteBaseUrl(),
                    generator.getSiteBanner(),
                    generator.getSiteCss(),
                    generator.getGAUid(),
                    mBlog.getTitle(),
                    destFile.getName(),  // page filename (for base-url/page-filename.html)
                    "", // prevPageLink,
                    "", // nextPageLink
                    mBlog.getBlogHeader().getFormatted(),
                    postData.mTitle,
                    postData.mDate.toString(),
                    generator.categoryToHtml(postData.mCategory),
                    generator.linkForCategory(postData.mCategory),
                    postData.mContent.getFormatted()
            );

            String generated = generator.getTemplater().generate(templateData);
            generator.getFileOps().writeBytes(generated.getBytes(Charsets.UTF_8), destFile);
        }
    }

    public static class PostShort implements Comparable<PostShort> {
        private final String mCategory;
        private final String mKey;
        private final LocalDate mDate;
        private final String mTitle;
        private final SourceTree.Content mContent;
        private final PostExtra mPostExtra;

        public PostShort(
                @NonNull String category,
                @NonNull String key,
                @NonNull LocalDate date,
                @NonNull String title,
                @NonNull SourceTree.Content content,
                @Null PostExtra postExtra) {
            mCategory = category;
            mKey = key;
            mDate = date;
            mTitle = title;
            mContent = content;
            mPostExtra = postExtra;
        }

        @Override
        public int compareTo(PostShort other) {
            return mKey.compareTo(other.mKey);
        }
    }

    public static class PostExtra implements Comparable<PostExtra> {
        private final FileItem mFileItem;
        private final SourceTree.Content mContent;
        private final String mCategory;
        private final String mKey;
        private final LocalDate mDate;
        private final String mTitle;

        public PostExtra(
                @NonNull BlogPage parent,
                @NonNull String category,
                @NonNull String key,
                @NonNull LocalDate date,
                @NonNull String title,
                @NonNull SourceTree.Content content) {
            mCategory = category;
            mKey = key;
            mDate = date;
            mTitle = title;
            mFileItem = new FileItem(parent.mFileItem.getDir(), mKey + HTML);
            mContent = content;
        }

        @Override
        public int compareTo(PostExtra other) {
            return mKey.compareTo(other.mKey);
        }
    }

    public static class FileItem {
        private final File mDir;
        private final String mName;

        public FileItem(@NonNull File dir, @NonNull String name) {
            mDir = dir;
            mName = name;
        }

        @NonNull
        public File getDir() {
            return mDir;
        }

        @NonNull
        public String getName() {
            return mName;
        }

        @NonNull
        public String getLeafDir() {
            String path = mDir.getPath();
            path = path.replace("..", "");
            return path;
        }

        @NonNull
        public String getLeafFile() {
            File file = new File(mDir, mName);
            String path = file.getPath();
            path = path.replace("..", "");
            return path;
        }
    }

}
