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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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
        BlogSites sites = parseSites(blogEntries);
        List<BlogSourceParser.ParsedResult> parsedResults = parseSources(sites, blogEntries);
        for (BlogSite blogSite : sites.iter()) {
            SourceTree sourceTree = computeSourceTree(blogSite, parsedResults);
            sourceTree.setChanged(allChanged);
            PostTree postTree = computePostTree(blogSite, sourceTree);
            generatePostTree(postTree);
            postTree.saveMetadata();
            sourceTree.saveMetadata();
        }
    }

    private SourceTree computeSourceTree(@NonNull BlogSite blogSite,
                                         @NonNull List<BlogSourceParser.ParsedResult> parsedResults)
            throws BlogSourceParser.ParseException {
        SourceTree sourceTree = new SourceTree();

        for (BlogSourceParser.ParsedResult parsedResult : parsedResults) {
            sourceTree.merge(parsedResult,
                parsedResult.isFileChanged(),
                blogSite.getCatAcceptFilter(),
                blogSite.getCatRejectFilter());
        }

        return sourceTree;
    }

    private BlogSites parseSites(List<BlogEntry> blogEntries) {
        BlogSites sites = new BlogSites();
        for (BlogEntry blogEntry : blogEntries) {
            sites.add(blogEntry.getSiteNumber());
        }
        return sites;
    }

    @NonNull
    private List<BlogSourceParser.ParsedResult> parseSources(@NonNull BlogSites sites,
                                                             @NonNull List<BlogEntry> blogEntries)
            throws IOException, URISyntaxException {
        List<BlogSourceParser.ParsedResult> parsedResults = new ArrayList<>();

        for (BlogEntry blogEntry : blogEntries) {
            BlogSourceParser.ParsedResult result = parseSource(blogEntry);
            parsedResults.add(result);

            BlogSite blogSite = sites.get(blogEntry);
            blogSite.updateFrom(result.getTags());
        }

        return parsedResults;
    }

    private BlogSourceParser.ParsedResult parseSource(@NonNull BlogEntry blogEntry)
            throws IOException, URISyntaxException {
        mLogger.d(TAG, "Parse section: " + blogEntry.getSiteNumber() + ", source: " + blogEntry.getFileId());
        GDocEntity entity = mGDocHelper.getGDocAsync(blogEntry.getFileId(), "text/html");
        boolean fileChanged = !entity.isUpdateToDate();
        byte[] content = entity.getContent();

        BlogSourceParser blogSourceParser = new BlogSourceParser(mHtmlTransformer);
        return blogSourceParser.parse(content).setFileChanged(fileChanged);
    }

    @NonNull
    private PostTree computePostTree(@NonNull BlogSite blogSite,
                                     @NonNull SourceTree sourceTree)
            throws BlogSourceParser.ParseException {
        mLogger.d(TAG, "computePostTree");
        PostTree postTree = new PostTree();

        // Generate per-category blogs
        if (!blogSite.getGenSingleFilter().isEmpty()) {
            for (SourceTree.Blog sourceBlog : sourceTree.getBlogs().values()) {
                if (blogSite.getGenSingleFilter().matches(sourceBlog.getCategory())) {
                    PostTree.Blog blog = createPostBlogFrom(sourceBlog);
                    postTree.add(blog);
                }
            }
        }

        // Generate mixed-categories blog
        if (!blogSite.getGenMixedFilter().isEmpty()) {
            SourceTree.Blog mixedSource = sourceTree.createMixedBlog(
                    blogSite.getMixedCat(),
                    blogSite.getGenMixedFilter());
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
        // - page 1 contains the oldest posts (sorted by date).
        // - page N contains the newer posts.
        // - index contains the most recent N posts and overlaps with page N/N-1 at any given time.
        // - page N is not generated if it is not full (as its posts are duplicated in the index).
        //
        // The rationale for doing this is that the page number of older posts keeps stable
        // as the blog grows, assuming there is no back-dating of posts.

        int pageCount = 1;
        List<PostTree.BlogPage> pages = new ArrayList<>();
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
                // TODO delay this till next post is added, to avoid the case where index==page1.
                pages.add(page);
            }
        }

        // Prepare the index
        indexPosts.sort(Comparator.reverseOrder());
        blog.getBlogIndex().fillFrom(indexPosts);

        // Add the pages in reverse order
        for (int i = pages.size() - 1; i >= 0; i--) {
            blog.getBlogPages().add(pages.get(i));
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

        public String getAbsSiteLink() {
            return mFlags.getString(EXP_SITE_BASE_URL);
        }

        public String getRelSiteLink() {
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
            if (category.length() == 3) {
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

    public class BlogSites {
        private final Map<Integer, BlogSite> mBlogSites = new TreeMap<>();

        public void add(int section) {
            mBlogSites.computeIfAbsent(section, (i) -> new BlogSite());
        }

        @NonNull
        public BlogSite get(@NonNull BlogEntry entry) {
            return Preconditions.checkNotNull(mBlogSites.get(entry.getSiteNumber()));
        }

        public Collection<BlogSite> iter() {
            return mBlogSites.values();
        }
    }

    /**
     * A "blog site" represents a full independent blog site. "Site" here means a configuration
     * section, with its own configuration variables: categories accept/reject, banner name,
     * and whether to generate single vs mixed pages. Various blogs can be generated from the
     * same site and they all use the same configuration variables.
     */
    public class BlogSite {
        private String mMixedCat;
        private final Map<String, CatFilter> mFilters = new HashMap<>();

        public BlogSite() {
            mMixedCat = mFlags.getString(BlogFlags.BLOG_MIXED_CAT);
            for (String flag : BlogFlags.FILTER_FLAGS) {
                mFilters.put(flag, new CatFilter(mFlags.getString(flag)));
            }
        }

        @SuppressWarnings("UnnecessaryLabelOnContinueStatement")
        public void updateFrom(@NonNull List<String> tags) {
            String izuMixedCatTag = IzuTags.PREFIX + BlogFlags.BLOG_MIXED_CAT + IzuTags.PARAM_SEP;

            nextTag: for (String tag : tags) {
                if (tag.startsWith(izuMixedCatTag)) {
                    String value = tag.substring(izuMixedCatTag.length()).trim();
                    if (!value.isEmpty()) {
                        mMixedCat = value;
                    }
                    continue nextTag;
                }

                for (String flag : BlogFlags.FILTER_FLAGS) {
                    String izuTag = IzuTags.PREFIX + flag + IzuTags.PARAM_SEP;

                    if (tag.startsWith(izuTag)) {
                        String value = tag.substring(izuTag.length()).trim();
                        if (!value.isEmpty()) {
                            mFilters.put(flag, new CatFilter(value));
                        }
                        continue nextTag;
                    }
                }
            }
        }

        public CatFilter getCatAcceptFilter() {
            return mFilters.get(BlogFlags.BLOG_ACCEPT_CAT);
        }

        public CatFilter getCatRejectFilter() {
            return mFilters.get(BlogFlags.BLOG_REJECT_CAT);
        }

        public CatFilter getCatBannerFilter() {
            return mFilters.get(BlogFlags.BLOG_BANNER_EXCLUDE);
        }

        public CatFilter getGenSingleFilter() {
            return mFilters.get(BlogFlags.BLOG_GEN_SINGLE);
        }

        public CatFilter getGenMixedFilter() {
            return mFilters.get(BlogFlags.BLOG_GEN_MIXED);
        }

        public String getMixedCat() {
            return mMixedCat;
        }
    }
}
