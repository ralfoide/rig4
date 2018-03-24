package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;

import javax.inject.Inject;
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

    public void processEntries(List<String> blogIds, boolean allChanged) {
        SourceTree sourceTree = parseSources(blogIds);
        sourceTree.setRootChanged(allChanged);
        PostTree postTree = computePostTree(sourceTree);
        postTree.generate();
        postTree.saveMetadata();
        sourceTree.saveMetadata();
    }

    @NonNull
    private SourceTree parseSources(List<String> blogIds) {
        SourceTree sourceTree = new SourceTree();

        for (String blogId : blogIds) {
            parseSource(sourceTree, blogId);
        }

        return sourceTree;
    }

    private void parseSource(SourceTree sourceTree, String blogId) {
        GDocEntity entity = mGDocHelper.getGDocAsync(blogId, "text/html");

    }

    @NonNull
    private PostTree computePostTree(SourceTree sourceTree) {
        return new PostTree();
    }


    private static class SourceTree {


        public void setRootChanged(boolean allChanged) {
        }

        public void saveMetadata() {
        }
    }

    private static class PostTree {

        public void generate() {
        }

        public void saveMetadata() {
        }
    }
}
