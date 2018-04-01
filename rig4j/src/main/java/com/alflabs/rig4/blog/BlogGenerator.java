package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.exp.HtmlTransformer;
import com.alflabs.rig4.exp.Templater;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BlogGenerator {
    private final static int ITEM_PER_PAGE = 10;

    private final ILogger mLogger;
    private final FileOps mFileOps;
    private final GDocHelper mGDocHelper;
    private final HashStore mHashStore;
    private final Templater mTemplater;
    private final HtmlTransformer mHtmlTransformer;

    @Inject
    public BlogGenerator(
            ILogger logger,
            FileOps fileOps,
            GDocHelper gDocHelper,
            HashStore hashStore,
            Templater templater,
            HtmlTransformer htmlTransformer) {
        mLogger = logger;
        mFileOps = fileOps;
        mGDocHelper = gDocHelper;
        mHashStore = hashStore;
        mTemplater = templater;
        mHtmlTransformer = htmlTransformer;
    }

    public void processEntries(@NonNull List<String> blogIds, boolean allChanged)
            throws IOException, URISyntaxException {
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
        GDocEntity entity = mGDocHelper.getGDocAsync(blogId, "text/html");
        boolean fileChanged = !entity.isUpdateToDate();
        byte[] content = entity.getContent();

        BlogSourceParser blogSourceParser = new BlogSourceParser(mHtmlTransformer);
        BlogSourceParser.ParsedResult result = blogSourceParser.parse(content);
        sourceTree.merge(result, fileChanged);
    }

    @NonNull
    private PostTree computePostTree(@NonNull SourceTree sourceTree) {
        PostTree postTree = new PostTree();
        for (SourceTree.Blog sourceBlog : sourceTree.getBlogs().values()) {
            PostTree.Blog blog = createPostBlogFrom(sourceBlog);
            postTree.add(blog);
        }
        return postTree;
    }

    @NonNull
    private PostTree.Blog createPostBlogFrom(@NonNull SourceTree.Blog sourceBlog) {
        PostTree.Blog blog = new PostTree.Blog(sourceBlog.getCategory(), sourceBlog.getHeaderContent());

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

                PostTree.BlogPage page = new PostTree.BlogPage(blog.getBlogIndex(), pageCount++);
                page.fillFrom(pendingPosts);
                pendingPosts.clear();
            }
        }

        // Prepare the index
        indexPosts.sort(Comparator.reverseOrder());
        blog.getBlogIndex().fillFrom(indexPosts);

        return blog;
    }

    private void generatePostTree(PostTree postTree) {
        postTree.generate();
    }


}
