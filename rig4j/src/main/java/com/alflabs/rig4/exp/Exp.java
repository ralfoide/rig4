package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.EntryPoint;
import com.alflabs.rig4.HashStore;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.rig4.struct.ArticleEntry;
import com.alflabs.rig4.struct.Index;
import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class Exp {
    private static final String TAG = Exp.class.getSimpleName();

    static final String EXP_DOC_ID = "exp-doc-id";
    static final String EXP_DEST_DIR = "exp-dest-dir";
    static final String EXP_GA_UID = "exp-ga-uid";
    static final String EXP_SITE_TITLE = "exp-site-title";
    static final String EXP_SITE_BANNER = "exp-site-banner";
    /** Base URL is expected to be in the format http(s)://some.host(/folder)/ with trailing slash. */
    static final String EXP_SITE_BASE_URL = "exp-site-base-url";

    private final Flags mFlags;
    private final ILogger mLogger;
    private final Timing mTiming;
    private final GDocHelper mGDocHelper;
    private final HashStore mHashStore;
    private final BlogGenerator mBlogGenerator;
    private final ArticleGenerator mArticleGenerator;

    @Inject
    public Exp(
            Flags flags,
            ILogger logger,
            Timing timing,
            GDocHelper gDocHelper,
            HashStore hashStore,
            BlogGenerator blogGenerator,
            ArticleGenerator articleGenerator) {
        mFlags = flags;
        mLogger = logger;
        mTiming = timing;
        mGDocHelper = gDocHelper;
        mHashStore = hashStore;
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

    public void start() throws IOException, URISyntaxException, InvocationTargetException, IllegalAccessException, ParseException {
        Timing.TimeAccumulator timing = mTiming.get("Total").start();
        boolean allChanged = checkVersionChanged();
        Index index = readIndex();
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


    private static final Pattern sArticleLineRe = Pattern.compile("^([a-z0-9_-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*");
    private static final Pattern sBlogLineRe    = Pattern.compile("^blog\\s+([a-zA-Z0-9_-]+)\\s*");

    @NonNull
    private Index readIndex() throws IOException {
        mLogger.d(TAG, "Processing document: index");
        String indexId = mFlags.getString(EXP_DOC_ID);
        GDocEntity entity = mGDocHelper.getGDoc(indexId, "text/plain");
        String content = new String(entity.getContent(), Charsets.UTF_8);

        List<ArticleEntry> entries = new ArrayList<>();
        List<String> blogIds = new ArrayList<>();

        for (String line : content.split("\n")) {
            line = line.trim();
            Matcher matcher = sArticleLineRe.matcher(line);
            if (matcher.find()) {
                entries.add(ArticleEntry.create(matcher.group(2), matcher.group(1)));
                continue;
            }
            matcher = sBlogLineRe.matcher(line);
            if (matcher.find()) {
                blogIds.add(matcher.group(1));
            }
        }

        return Index.create(entries, blogIds);
    }
}
