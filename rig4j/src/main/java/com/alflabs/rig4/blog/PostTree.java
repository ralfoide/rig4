package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.exp.Templater;
import com.google.common.base.Charsets;

import java.io.File;
import java.io.IOException;
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
    private final static String BLOG_ROOT = "blog";
    private static final String HTML = ".html";
    private static final String ATOM_XML = "atom.xml";
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
            mBlogHeader = blogHeader != null ? blogHeader : new SourceTree.Content();
            mBlogIndex = new BlogPage(this, new File(BLOG_ROOT, category));
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

            AtomWriter atomWriter = new AtomWriter();
            atomWriter.write(this, generator, new FileItem(mBlogIndex.getFileItem().getDir(), ATOM_XML));

            PostFull nextFull = null;
            for (int i = mBlogPages.size() - 1; i >= 0; i--) {
                nextFull = mBlogPages.get(i).computeNextFullPage(nextFull);
            }

            PostFull prevFull = null;
            for (int i = 0; i < mBlogPages.size(); i++) {
                prevFull = mBlogPages.get(i).computePrevFullPage(prevFull);
                mBlogPages.get(i).generate(i, mBlogPages, generator);
            }
        }
    }

    public static class BlogPage {
        private final Blog mBlog;
        private final FileItem mFileItem;
        private final List<PostShort> mPostShorts = new ArrayList<>();
        private final List<PostFull> mPostFulls = new ArrayList<>();

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

        public List<PostFull> getPostFulls() {
            return mPostFulls;
        }

        /**
         * Fill the page with the given posts.
         * The input collection should already be ordered as it should be presented on the page.
         */
        public void fillFrom(@NonNull Collection<SourceTree.BlogPost> sourcePosts) {
            for (SourceTree.BlogPost sourcePost : sourcePosts) {
                SourceTree.Content fullContent = sourcePost.getFullContent();
                SourceTree.Content shortContent = sourcePost.getShortContent();
                boolean readMoreLink = true;
                if (shortContent == null) {
                    shortContent = fullContent;
                    readMoreLink = false;
                }

                PostFull postFull = new PostFull(
                        this,
                        sourcePost.getCategory(),
                        sourcePost.getKey(),
                        sourcePost.getDate(),
                        sourcePost.getTitle(),
                        fullContent);

                PostShort postShort = new PostShort(
                        sourcePost.getCategory(),
                        sourcePost.getKey(),
                        sourcePost.getDate(),
                        sourcePost.getTitle(),
                        shortContent,
                        postFull,
                        readMoreLink);

                mPostFulls .add(postFull);
                mPostShorts.add(postShort);
            }

            // The posts comparator compares using ascending keys. We want to descending order.
            mPostFulls .sort(Collections.reverseOrder());
            mPostShorts.sort(Collections.reverseOrder());
        }

        public void generate(int index,
                             @NonNull List<BlogPage> blogPages,
                             @NonNull BlogGenerator.Generator generator) throws Exception {
            // Write one file with all the short entries.
            File mainFile = generateMainPage(index, blogPages, generator);

            // Write one file per full entry.
            for (PostFull postFull : mPostFulls) {
                generateFullPage(generator, postFull, mainFile);
            }
        }

        public PostFull computePrevFullPage(PostFull lastFull) {
            for (PostFull postFull : mPostFulls) {
                postFull.mPrevFull = lastFull;
                lastFull = postFull;
            }
            return lastFull;
        }

        public PostFull computeNextFullPage(PostFull lastFull) {
            for (int i = mPostFulls.size() - 1; i >= 0; i--) {
                PostFull postFull = mPostFulls.get(i);
                postFull.mNextFull = lastFull;
                lastFull = postFull;
            }
            return lastFull;
        }

        private File generateMainPage(int index,
                                      @NonNull List<BlogPage> blogPages,
                                      @NonNull BlogGenerator.Generator generator) throws Exception {
            File destFile = new File(generator.getDestDir(), mFileItem.getLeafFile());

            generator.getFileOps().createParentDirs(destFile);
            generator.getLogger().d(TAG, "--- Generate  Page: " + generator.categoryToHtml(mBlog.getCategory())
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

            Templater.BlogPageData templateData = new Templater.BlogPageData(
                    generator.getSiteTitle(),
                    generator.getAbsSiteLink(),
                    mFileItem.getLeafDirWeb(),
                    generator.getRelBannerLink(),
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
                    content.toString(),
                    generator.getGenInfo(),
                    "" /*relImageLink*/);

            String generated = generator.getTemplater().generate(templateData);
            generator.getFileOps().writeBytes(generated.getBytes(Charsets.UTF_8), destFile);

            return destFile;
        }

        private String generateShort(
                @NonNull BlogGenerator.Generator generator,
                @NonNull File destFile,
                @NonNull PostShort postData)
                throws Exception {
            generator.getLogger().d(TAG, "    Generate Short: " + postData.mKey);

            postData.mContent.setTransformer(generator.getLazyHtmlTransformer(destFile));

            String fullLink = postData.mPostFull.mFileItem.getName();

            Templater.BlogPostData templateData = new Templater.BlogPostData(
                    generator.getAbsSiteLink(),
                    mFileItem.getLeafDirWeb(),
                    postData.mTitle,
                    postData.mDate.toString(),
                    generator.categoryToHtml(postData.mCategory),
                    generator.linkForCategory(postData.mCategory),
                    fullLink,
                    postData.mReadMoreLink ? fullLink : null,
                    postData.mContent.getFormatted()
            );

            return generator.getTemplater().generate(templateData);
        }

        private void generateFullPage(
                @NonNull BlogGenerator.Generator generator,
                @NonNull PostFull postData,
                @NonNull File mainFile)
                throws Exception {
            File destFile = postData.prepareHtmlDestFile(mBlog, generator);

            generator.getLogger().d(TAG, "    Generate  Full: " + postData.mKey);

            String prevPageLink = postData.mPrevFull == null
                    ? null
                    : postData.mPrevFull.mFileItem.getName();
            String nextPageLink = postData.mNextFull == null
                    ? null
                    : postData.mNextFull.mFileItem.getName();

            String content = postData.mContent.getFormatted();
            String relImageLink = postData.mContent.getFirstFormattedImageSrc();

            Templater.BlogPageData templateData = new Templater.BlogPageData(
                    generator.getSiteTitle(),
                    generator.getAbsSiteLink(),
                    mFileItem.getLeafDirWeb(),
                    generator.getRelBannerLink(),
                    generator.getSiteCss(),
                    generator.getGAUid(),
                    mBlog.getTitle(),
                    mainFile.getName(),  // main page links to the index containing this full page
                    prevPageLink,
                    nextPageLink,
                    mBlog.getBlogHeader().getFormatted(),
                    postData.mTitle,
                    postData.mDate.toString(),
                    generator.categoryToHtml(postData.mCategory),
                    generator.linkForCategory(postData.mCategory),
                    content,
                    generator.getGenInfo(),
                    relImageLink
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
        private final PostFull mPostFull;
        private final boolean mReadMoreLink;

        public PostShort(
                @NonNull String category,
                @NonNull String key,
                @NonNull LocalDate date,
                @NonNull String title,
                @NonNull SourceTree.Content content,
                @NonNull PostFull postFull,
                boolean readMoreLink) {
            mCategory = category;
            mKey = key;
            mDate = date;
            mTitle = title;
            mContent = content;
            mPostFull = postFull;
            mReadMoreLink = readMoreLink;
        }

        @Override
        public int compareTo(PostShort other) {
            return mKey.compareTo(other.mKey);
        }
    }

    public static class PostFull implements Comparable<PostFull> {
        public final FileItem mFileItem;
        public final SourceTree.Content mContent;
        public final String mCategory;
        public final String mKey;
        public final LocalDate mDate;
        public final String mTitle;
        private PostFull mPrevFull;
        private PostFull mNextFull;

        public PostFull(
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
        public int compareTo(PostFull other) {
            return mKey.compareTo(other.mKey);
        }

        public File prepareHtmlDestFile(Blog blog, BlogGenerator.Generator generator) throws IOException {
            File destFile = new File(generator.getDestDir(), this.mFileItem.getLeafFile());
            generator.getFileOps().createParentDirs(destFile);

            this.mContent.setTransformer(generator.getLazyHtmlTransformer(destFile));
            blog.getBlogHeader().setTransformer(generator.getLazyHtmlTransformer(destFile));
            return destFile;
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
        public String getLeafDirWeb() {
            String path = getLeafDir().replace(File.separatorChar, '/');
            if (!path.endsWith("/")) {
                path += "/";
            }
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
