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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;

@Singleton
public class Exp {
    private static final String TAG = Exp.class.getSimpleName();

    public static final String EXP_DOC_ID = "exp-doc-id";
    public static final String EXP_DEST_DIR = "exp-dest-dir";
    public static final String EXP_GA_UID = "exp-ga-uid";
    public static final String EXP_SITE_TITLE = "exp-site-title";
    public static final String EXP_SITE_BANNER = "exp-site-banner";
    /** Base URL is expected to be in the format http(s)://some.host(/folder)/ with trailing slash. */
    public static final String EXP_SITE_BASE_URL = "exp-site-base-url";

    private final Flags mFlags;
    private final ILogger mLogger;
    private final Timing mTiming;
    private final HashStore mHashStore;
    private final IndexReader mIndexReader;
    private final BlogGenerator mBlogGenerator;
    private final ArticleGenerator mArticleGenerator;

    @Inject
    public Exp(
            Flags flags,
            ILogger logger,
            Timing timing,
            HashStore hashStore,
            IndexReader indexReader,
            BlogGenerator blogGenerator,
            ArticleGenerator articleGenerator) {
        mFlags = flags;
        mLogger = logger;
        mTiming = timing;
        mHashStore = hashStore;
        mIndexReader = indexReader;
        mBlogGenerator = blogGenerator;
        mArticleGenerator = articleGenerator;
    }

    public void declareFlags() {
        mFlags.addString(EXP_DOC_ID,        "",           "Exp gdoc id");
        mFlags.addString(EXP_DEST_DIR,      "",           "Exp dest dir");
        mFlags.addString(EXP_GA_UID,        "",           "Exp GA UID");
        mFlags.addString(EXP_SITE_TITLE,    "Site Title", "Web site title");
        mFlags.addString(EXP_SITE_BANNER,   "header.jpg", "Web site banner filename");
        mFlags.addString(EXP_SITE_BASE_URL, "http://localhost/folder/", "Web site base URL");
    }

    public void start() throws Exception {
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
