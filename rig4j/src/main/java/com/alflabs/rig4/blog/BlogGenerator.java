package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.EntryPoint;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.exp.HtmlTransformer;
import com.alflabs.rig4.exp.Templater;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.BlogEntry;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;
import com.google.common.base.Preconditions;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.alflabs.rig4.exp.ExpFlags.EXP_DEST_DIR;
import static com.alflabs.rig4.exp.ExpFlags.EXP_GA_UID;
import static com.alflabs.rig4.exp.ExpFlags.EXP_SITE_BANNER;
import static com.alflabs.rig4.exp.ExpFlags.EXP_SITE_BASE_URL;
import static com.alflabs.rig4.exp.ExpFlags.EXP_SITE_TITLE;

public class BlogGenerator {
    private final static String TAG = BlogGenerator.class.getSimpleName();
    private final static int ITEM_PER_PAGE = 10;

    private final Flags mFlags;
    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final GDocHelper mGDocHelper;
    private final HashStore mHashStore;
    private final Templater mTemplater;
    private final HtmlTransformer mHtmlTransformer;

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

    public void processEntries(@NonNull List<BlogEntry> blogEntries, boolean allChanged)
            throws Exception {
        BlogConfigs configs = parseConfigs(blogEntries);
        List<BlogSourceParser.ParsedResult> parsedResults = parseSources(configs, blogEntries);
        for (BlogConfig blogConfig : configs.iter()) {
            SourceTree sourceTree = computeSourceTree(blogConfig, parsedResults);
            if (allChanged || sourceTree.isModified()) {
                PostTree postTree = computePostTree(blogConfig, sourceTree);
                generatePostTree(postTree);
                postTree.saveMetadata();
                sourceTree.saveMetadata();
            } else {
                mLogger.d(TAG, "  Unchanged: " + blogConfig.getMixedCat());
            }
        }
    }

    private SourceTree computeSourceTree(@NonNull BlogConfig blogConfig,
                                         @NonNull List<BlogSourceParser.ParsedResult> parsedResults)
            throws BlogSourceParser.ParseException {
        SourceTree sourceTree = new SourceTree();

        int modified = 0;
        for (BlogSourceParser.ParsedResult parsedResult : parsedResults) {
            sourceTree.merge(parsedResult,
                parsedResult.isFileChanged(),
                blogConfig.getCatAcceptFilter(),
                blogConfig.getCatRejectFilter());
            if (parsedResult.isFileChanged()) { modified++; }
        }
        mLogger.d(TAG, "  " + parsedResults.size() + " source entries, " + modified + " modified");

        return sourceTree;
    }

    /**
     * Initializes the config map in {@link BlogConfigs} based on the {@link BlogEntry}s
     * defined in the index file.
     */
    private BlogConfigs parseConfigs(List<BlogEntry> blogEntries) {
        BlogConfigs configs = new BlogConfigs();
        for (BlogEntry blogEntry : blogEntries) {
            configs.add(mFlags, blogEntry.getConfigNumber());
        }
        return configs;
    }

    @NonNull
    private List<BlogSourceParser.ParsedResult> parseSources(@NonNull BlogConfigs configs,
                                                             @NonNull List<BlogEntry> blogEntries)
            throws IOException, URISyntaxException {
        List<BlogSourceParser.ParsedResult> parsedResults = new ArrayList<>();

        for (BlogEntry blogEntry : blogEntries) {
            BlogSourceParser.ParsedResult result = parseSource(blogEntry);
            parsedResults.add(result);

            BlogConfig blogConfig = configs.get(blogEntry);
            blogConfig.updateFrom(result.getTags());
        }

        return parsedResults;
    }

    private BlogSourceParser.ParsedResult parseSource(@NonNull BlogEntry blogEntry)
            throws IOException, URISyntaxException {
        mLogger.d(TAG, "Parse config " + blogEntry.getConfigNumber() + ", source: " + blogEntry.getFileId());
        GDocEntity entity = mGDocHelper.getGDocAsync(blogEntry.getFileId(), "text/html");
        boolean fileChanged = !entity.isUpdateToDate();
        byte[] content = entity.getContent();
        Preconditions.checkNotNull(content); // fail fast
        entity.syncToStore();

        BlogSourceParser blogSourceParser = new BlogSourceParser(mHtmlTransformer);
        return blogSourceParser.parse(content).setFileChanged(blogEntry.getFileId(), fileChanged);
    }

    @NonNull
    private PostTree computePostTree(@NonNull BlogConfig blogConfig,
                                     @NonNull SourceTree sourceTree)
            throws BlogSourceParser.ParseException {
        mLogger.d(TAG, "computePostTree");
        PostTree postTree = new PostTree();

        // Generate per-category blogs
        if (!blogConfig.getGenSingleFilter().isEmpty()) {
            for (SourceTree.Blog sourceBlog : sourceTree.getBlogs().values()) {
                if (blogConfig.getGenSingleFilter().matches(sourceBlog.getCategory())) {
                    PostTree.Blog blog = createPostBlogFrom(sourceBlog);
                    postTree.add(blog);
                }
            }
        }

        // Generate mixed-categories blog
        if (!blogConfig.getGenMixedFilter().isEmpty()) {
            SourceTree.Blog mixedSource = sourceTree.createMixedBlog(
                    blogConfig.getMixedCat(),
                    blogConfig.getGenMixedFilter());
            PostTree.Blog mixed = createPostBlogFrom(mixedSource);
            postTree.add(mixed);
        }

        return postTree;
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
        // - index contains the most recent N posts.
        // - page 1 contains next N oldest posts, after the index.
        // - pages 1..M-1 contain N posts each.
        // - page M contains the oldest of the oldest post. amd may have less than N posts.

        int pageCount = 0;
        List<PostTree.BlogPage> pages = new ArrayList<>();
        List<SourceTree.BlogPost> indexPosts = new ArrayList<>();
        List<SourceTree.BlogPost> pendingPosts = new ArrayList<>();
        // Note: sourcePosts should be "naturally" ordered from older to newer due to the treemap.
        // See also the fixed postShorts order in BlogPage.

        // Get all the posts in reverse chronological order (newer first) by getting the posts
        // in chronological order and then reading the list backwards.
        ArrayList<SourceTree.BlogPost> posts = new ArrayList<>(sourceBlog.getPosts());
        posts.sort(Comparator.naturalOrder());
        int numPerPage = 0;
        for (int i = posts.size() - 1; i >= 0; i--) {
            SourceTree.BlogPost sourcePost = posts.get(i);
            boolean isLast = i == 0;

            if (pageCount == 0) {
                indexPosts.add(sourcePost);
            } else {
                pendingPosts.add(sourcePost);
            }

            numPerPage++;
            if (numPerPage == ITEM_PER_PAGE || isLast) {
                // Generate page when pending list is full or is the last post.
                if (pageCount > 0) {
                    // Keep the posts list reverse-ordered (from most recent to older one).
                    pendingPosts.sort(Comparator.reverseOrder());

                    PostTree.BlogPage page = new PostTree.BlogPage(blog, blog.getBlogIndex(), pageCount);
                    page.fillFrom(pendingPosts);
                    pages.add(page);
                    pendingPosts.clear();
                }

                pageCount++;
                numPerPage = 0;
            }
        }

        // Prepare the index
        indexPosts.sort(Comparator.reverseOrder());
        blog.getBlogIndex().fillFrom(indexPosts);

        // Add the pages in normal order
        for (PostTree.BlogPage page : pages) {
            blog.getBlogPages().add(page);
        }

        mLogger.d(TAG, "Blog " + blog.getCategory()
                + ", posts: " + sourceBlog.getPosts().size()
                + ", pages: " + blog.getBlogPages().size());
        return blog;
    }

    private void generatePostTree(PostTree postTree) throws Exception {
        mLogger.d(TAG, "generatePostTree");
        postTree.generate(new Generator(postTree));
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
        private final PostTree mPostTree;

        public Generator(PostTree postTree) {
            mPostTree = postTree;
        }

        public HtmlTransformer.LazyTransformer getLazyHtmlTransformer(File destFile, @NonNull String transformKey) {
            HtmlTransformer.Callback callback = new HtmlTransformer.Callback() {
                @Override
                public String processDrawing(String id, int width, int height, boolean useCache) throws IOException {
                    return mGDocHelper.downloadDrawing(id, destFile, width, height, useCache);
                }

                @Override
                public String processImage(URI uri, int width, int height, boolean useCache) throws IOException {
                    return mGDocHelper.downloadImage(uri, destFile, width, height, useCache);
                }
            };
            // Transformer key is the full path of the file generated. If a post is reused in
            // a different directory or different name, its assets should be regenerated for that
            // directory.
            return mHtmlTransformer.createLazyTransformer(transformKey + destFile.getPath(), callback);
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

        public String getAbsSiteLink() {
            return mFlags.getString(EXP_SITE_BASE_URL);
        }

        /** A reverse web link to go from a generated blog page to the site's root. */
        public String getRevSiteLink() {
            // There are always two levels in the generated pages: <site>/blog/cat/<content>.
            return "../../";
        }

        public String getRelBannerLink() {
            return mFlags.getString(EXP_SITE_BANNER);
        }

        public String getGenInfo() throws IOException {
            return "Generated on " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    + " by Rig4j " + EntryPoint.getVersion();
        }

        /** Transforms the category into what we want for the template, mainly capitalize it. */
        @NonNull
        public String categoryToHtml(@NonNull String category) {
            // Most of the time a 3-letter category is going to be an acronym.
            if (category.length() == 3 || "cmrs".equals(category)) {
                return category.toUpperCase(Locale.US);
            }
            // Otherwise use a simple capitalization.
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
