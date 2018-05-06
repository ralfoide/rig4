package com.alflabs.rig4.exp;

import com.alflabs.rig4.EntryPoint;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.blog.BlogGenerator;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.rig4.struct.Index;
import com.alflabs.utils.ILogger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

import static com.alflabs.rig4.exp.ExpFlags.*;

@Singleton
public class Exp {
    private static final String TAG = Exp.class.getSimpleName();

    private final Flags mFlags;
    private final ExpFlags mExpFlags;
    private final ILogger mLogger;
    private final Timing mTiming;
    private final HashStore mHashStore;
    private final IndexReader mIndexReader;
    private final BlogGenerator mBlogGenerator;
    private final ArticleGenerator mArticleGenerator;

    @Inject
    public Exp(
            Flags flags,
            ExpFlags expFlags,
            ILogger logger,
            Timing timing,
            HashStore hashStore,
            IndexReader indexReader,
            BlogGenerator blogGenerator,
            ArticleGenerator articleGenerator) {
        mFlags = flags;
        mExpFlags = expFlags;
        mLogger = logger;
        mTiming = timing;
        mHashStore = hashStore;
        mIndexReader = indexReader;
        mBlogGenerator = blogGenerator;
        mArticleGenerator = articleGenerator;
    }

    public void start() throws Exception {
        // Sanity check...
        if (!mFlags.getString(EXP_SITE_BASE_URL).endsWith("/")) {
            throw new IllegalArgumentException("Error: URL for " + EXP_SITE_BASE_URL + " needs to terminate with a /. Current value: " + mFlags.getString(EXP_SITE_BASE_URL));
        }

        Timing.TimeAccumulator timing = mTiming.get("Total").start();
        boolean allChanged = checkVersionChanged();
        Index index = mIndexReader.readIndex(mFlags.getString(EXP_DOC_ID));
        mArticleGenerator.processEntries(index.getArticleEntries(), allChanged);
        mBlogGenerator.processEntries(index.getBlogIds(), allChanged);
        timing.end();
        mTiming.printToLog();
    }

    // ---

    private boolean checkVersionChanged() throws IOException {
        final String versionKey = "version";

        String currVersion = EntryPoint.getVersion();

        boolean changed = !currVersion.equals(mHashStore.getString(versionKey));
        if (changed) {
            mHashStore.putString(versionKey, currVersion);
            mLogger.d(TAG, "Regenerating for new rig4j version: " + currVersion);
        }

        return changed;
    }


}
