package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.exp.HtmlTransformer;
import com.alflabs.rig4.exp.Templater;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.alflabs.rig4.exp.ExpFlags.EXP_DEST_DIR;
import static com.alflabs.rig4.exp.ExpFlags.EXP_GA_UID;
import static com.alflabs.rig4.exp.ExpFlags.EXP_SITE_BANNER;
import static com.alflabs.rig4.exp.ExpFlags.EXP_SITE_BASE_URL;
import static com.alflabs.rig4.exp.ExpFlags.EXP_SITE_TITLE;

public class BlogGenerator {
    private final static String TAG = BlogGenerator.class.getSimpleName();
    private final static int ITEM_PER_PAGE = 10;
    private static final String MIXED_CATEGORY = "all";

    private final Flags mFlags;
    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final GDocHelper mGDocHelper;
    private final HashStore mHashStore;
    private final Templater mTemplater;
    private final HtmlTransformer mHtmlTransformer;

    private CatFilter mCatAcceptFilter;
    private CatFilter mCatRejectFilter;
    private CatFilter mCatBannerFilter;
    private CatFilter mGenSingleFilter;
    private CatFilter mGenMixedFilter;
    private PostTree mPostTree;

    @Inject
    public BlogGenerator(
            Flags flags,
            ILogger logger,
            FileOps fileOps,
            GDocHelper gDocHelper,
            HashStore hashStore,
            Templater templater,
            HtmlTransformer htmlTransformer) {
        mFlags = flags;
        mLogger = logger;
        mFileOps = fileOps;
        mGDocHelper = gDocHelper;
        mHashStore = hashStore;
        mTemplater = templater;
        mHtmlTransformer = htmlTransformer;
    }

    public void processEntries(@NonNull List<String> blogIds, boolean allChanged)
            throws Exception {
        mCatAcceptFilter = new CatFilter(mFlags.getString(BlogFlags.BLOG_ACCEPT_CAT));
        mCatRejectFilter = new CatFilter(mFlags.getString(BlogFlags.BLOG_REJECT_CAT));
        mCatBannerFilter = new CatFilter(mFlags.getString(BlogFlags.BLOG_BANNER_EXCLUDE));
        mGenSingleFilter = new CatFilter(mFlags.getString(BlogFlags.BLOG_GEN_SINGLE));
        mGenMixedFilter  = new CatFilter(mFlags.getString(BlogFlags.BLOG_GEN_MIXED));

        SourceTree sourceTree = parseSources(blogIds);
        sourceTree.setChanged(allChanged);
        PostTree postTree = computePostTree(sourceTree);
        generatePostTree(postTree);
        postTree.saveMetadata();
        sourceTree.saveMetadata();
    }

    @NonNull
    private SourceTree parseSources(@NonNull List<String> blogIds)
            throws IOException, URISyntaxException {
        SourceTree sourceTree = new SourceTree();

        for (String blogId : blogIds) {
            parseSource(sourceTree, blogId);
        }

        return sourceTree;
    }

    private void parseSource(@NonNull SourceTree sourceTree, @NonNull String blogId)
            throws IOException, URISyntaxException {
        mLogger.d(TAG, "Parse source: " + blogId);
        GDocEntity entity = mGDocHelper.getGDocAsync(blogId, "text/html");
        boolean fileChanged = !entity.isUpdateToDate();
        byte[] content = entity.getContent();

        BlogSourceParser blogSourceParser = new BlogSourceParser(mHtmlTransformer);
        BlogSourceParser.ParsedResult result = blogSourceParser.parse(content);
        sourceTree.merge(result, fileChanged, mCatAcceptFilter, mCatRejectFilter);
    }

    @NonNull
    private PostTree computePostTree(@NonNull SourceTree sourceTree)
            throws BlogSourceParser.ParseException {
        mLogger.d(TAG, "computePostTree");
        mPostTree = new PostTree();

        // Generate per-category blogs
        if (!mGenSingleFilter.isEmpty()) {
            for (SourceTree.Blog sourceBlog : sourceTree.getBlogs().values()) {
                if (mGenSingleFilter.matches(sourceBlog.getCategory())) {
                    PostTree.Blog blog = createPostBlogFrom(sourceBlog);
                    mPostTree.add(blog);
                }
            }
        }

        // Generate mixed-categories blog
        if (!mGenMixedFilter.isEmpty()) {
            PostTree.Blog mixed = createPostBlogFrom(sourceTree.createMixedBlog(MIXED_CATEGORY, mGenMixedFilter));
            mPostTree.add(mixed);
        }

        return mPostTree;
    }

    @NonNull
    private PostTree.Blog createPostBlogFrom(@NonNull SourceTree.Blog sourceBlog) {
        PostTree.Blog blog = new PostTree.Blog(
                sourceBlog.getCategory(),
                sourceBlog.getTitle(),
                sourceBlog.getHeaderContent());

        // EXPERIMENTAL SIMPLIFICATION
        // ---------------------------
        // This creates a "reversed date" order blog:
        // - page 1 contains the oldest posts (sorted by date).
        // - page N contains the newer posts.
        // - index contains the most recent N posts and overlaps with page N/N-1 at any given time.
        // - page N is not generated if it is not full (as its posts are duplicated in the index).
        //
        // The rationale for doing this is that the page number of older posts keeps stable
        // as the blog grows, assuming there is no back-dating of posts.

        int pageCount = 1;
        List<SourceTree.BlogPost> indexPosts = new ArrayList<>();
        List<SourceTree.BlogPost> pendingPosts = new ArrayList<>();
        // Note: sourcePosts should be "naturally" ordered from older to newer due to the treemap.
        // See also the fixed postShorts order in BlogPage.
        for (SourceTree.BlogPost sourcePost : sourceBlog.getPosts()) {
            pendingPosts.add(sourcePost);

            // Keep the last N items for the index
            indexPosts.add(sourcePost);
            if (indexPosts.size() > ITEM_PER_PAGE) {
                indexPosts.remove(0);
            }

            // Generate page if pending list is full.
            if (pendingPosts.size() == ITEM_PER_PAGE) {
                // Keep the posts list reverse-ordered (from most recent to older one).
                pendingPosts.sort(Comparator.reverseOrder());

                PostTree.BlogPage page = new PostTree.BlogPage(blog, blog.getBlogIndex(), pageCount++);
                page.fillFrom(pendingPosts);
                pendingPosts.clear();
                blog.getBlogPages().add(page);
            }
        }

        // Prepare the index
        indexPosts.sort(Comparator.reverseOrder());
        blog.getBlogIndex().fillFrom(indexPosts);

        mLogger.d(TAG, "Blog " + blog.getCategory()
                + ", posts: " + sourceBlog.getPosts().size()
                + ", pages: " + blog.getBlogPages().size());
        return blog;
    }

    private void generatePostTree(PostTree postTree) throws Exception {
        mLogger.d(TAG, "generatePostTree");
        postTree.generate(new Generator());
    }

    /**
     * Generator used by {@link PostTree} to actually generate HTML files out of posts and pages.
     * This serves as a way to provide site-level configuration & data (e.g. site name, banner URL).
     * The HTML transformer is provided here as it both depends on the drawing/images helpers (which
     * PostTree does not need to be aware of) yet the transformer is keyed on the output file (for
     * generating the image assets). Thus each post & blog header has its own custom transformer
     * depending on the page where it is used.
     */
    public class Generator {
        public HtmlTransformer.LazyTransformer getLazyHtmlTransformer(File destFile) {
            HtmlTransformer.Callback callback = new HtmlTransformer.Callback() {
                @Override
                public String processDrawing(String id, int width, int height) throws IOException {
                    return mGDocHelper.downloadDrawing(id, destFile, width, height);
                }

                @Override
                public String processImage(URI uri, int width, int height) throws IOException {
                    return mGDocHelper.downloadImage(uri, destFile, width, height);
                }
            };
            // Transformer key is the directory of the file generated. If a post is reused in
            // a different directory, its assets should be regenerated for that directory.
            return mHtmlTransformer.createLazyTransformer(destFile.getParent(), callback);
        }

        public ILogger getLogger() {
            return mLogger;
        }

        public FileOps getFileOps() {
            return mFileOps;
        }

        public File getDestDir() {
            return new File(mFlags.getString(EXP_DEST_DIR));
        }

        public Templater getTemplater() {
            return mTemplater;
        }

        public String getSiteCss() {
            return "";
        }

        public String getGAUid() {
            return mFlags.getString(EXP_GA_UID);
        }

        public String getSiteTitle() {
            return mFlags.getString(EXP_SITE_TITLE);
        }

        public String getSiteBaseUrl() {
            return mFlags.getString(EXP_SITE_BASE_URL);
        }

        public String getSiteBanner() {
            return mFlags.getString(EXP_SITE_BANNER);
        }

        /** Transforms the category into what we want for the template, mainly capitalize it. */
        @NonNull
        public String categoryToHtml(@NonNull String category) {
            return StringUtils.capitalize(category);
        }

        /**
         * Generates the link added in a post for a category.
         * That link should only be generated if per-category pages are created for it.
         * Can be null to remove the link.
         */
        @Null
        public String linkForCategory(@NonNull String category) {
            PostTree.Blog blog = mPostTree.get(category);
            if (blog == null) {
                return  null;
            }
            return blog.getBlogIndex().getFileItem().getLeafDir();
        }
    }
}
