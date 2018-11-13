package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.gdoc.GDocHelper;
import com.alflabs.rig4.struct.ArticleEntry;
import com.alflabs.rig4.struct.BlogEntry;
import com.alflabs.rig4.struct.GDocEntity;
import com.alflabs.rig4.struct.Index;
import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexReader {
    private static final String TAG = IndexReader.class.getSimpleName();

    private final ILogger mLogger;
    private final GDocHelper mGDocHelper;

    @Inject
    public IndexReader(
            ILogger logger,
            GDocHelper gDocHelper) {
        mLogger = logger;
        mGDocHelper = gDocHelper;
    }

    private static final Pattern sArticleLineRe =
            Pattern.compile("^([a-z0-9_/-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*");
    private static final Pattern sBlogLineRe    =
            Pattern.compile("^[bB]log\\s*([1-9]+)?\\s*(\\([^)]*\\))?\\s+([a-zA-Z0-9_-]+)\\s*");

    @NonNull
    public Index readIndex(String indexId) throws IOException {
        mLogger.d(TAG, "Processing document: index " + indexId);
        GDocEntity entity = mGDocHelper.getGDocSync(indexId, "text/plain");
        String content = new String(entity.getContent(), Charsets.UTF_8);

        List<ArticleEntry> articleEntries = new ArrayList<>();
        List<BlogEntry> blogEntries = new ArrayList<>();

        for (String line : content.split("\n")) {
            line = line.trim();
            Matcher matcher = sArticleLineRe.matcher(line);
            if (matcher.find()) {
                articleEntries.add(ArticleEntry.create(matcher.group(2), matcher.group(1)));
                continue;
            }
            matcher = sBlogLineRe.matcher(line);
            if (matcher.find()) {
                int siteNumber = 0;
                try {
                    siteNumber = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignore) {}

                blogEntries.add(BlogEntry.create(matcher.group(3), siteNumber));
            }
        }

        return Index.create(articleEntries, blogEntries);
    }
}
