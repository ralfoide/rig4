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
import java.util.List;

public class BlogGenerator {

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

    public void processEntries(List<String> blogIds, boolean allChanged)
            throws IOException, URISyntaxException {
        SourceTree sourceTree = parseSources(blogIds);
        sourceTree.setRootChanged(allChanged);
        PostTree postTree = computePostTree(sourceTree);
        postTree.generate();
        postTree.saveMetadata();
        sourceTree.saveMetadata();
    }

    @NonNull
    private SourceTree parseSources(List<String> blogIds)
            throws IOException, URISyntaxException {
        SourceTree sourceTree = new SourceTree();

        for (String blogId : blogIds) {
            parseSource(sourceTree, blogId);
        }

        return sourceTree;
    }

    private void parseSource(SourceTree sourceTree, String blogId)
            throws IOException, URISyntaxException {
        GDocEntity entity = mGDocHelper.getGDocAsync(blogId, "text/html");
        entity.isUpdateToDate(); // TODO use if false
        byte[] content = entity.getContent();

        BlogSourceParser blogSourceParser = new BlogSourceParser(mHtmlTransformer);
        blogSourceParser.parse(content);
    }

    @NonNull
    private PostTree computePostTree(SourceTree sourceTree) {
        return new PostTree();
    }


}
