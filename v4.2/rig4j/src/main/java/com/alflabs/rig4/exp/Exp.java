package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.rig4.BlobStore;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class Exp {
    private static final String TAG = Exp.class.getSimpleName();

    private static final String EXP_DOC_ID = "exp-doc-id";
    private static final String EXP_DEST_DIR = "exp-dest-dir";
    private static final String EXP_GA_UID = "exp-ga-uid";

    private final Flags mFlags;
    private final ILogger mLogger;
    private final GDocReader mGDocReader;
    private final BlobStore mBlobStore;

    @Inject
    public Exp(Flags flags, ILogger logger, GDocReader gDocReader, BlobStore blobStore) {
        mFlags = flags;
        mLogger = logger;
        mGDocReader = gDocReader;
        mBlobStore = blobStore;
    }

    public void declareFlags() {
        mFlags.addString(EXP_DOC_ID, "", null);
        mFlags.addString(EXP_DEST_DIR, "", null);
        mFlags.addString(EXP_GA_UID, "", null);
    }

    public void start() throws IOException {
        List<HtmlEntry> entries = readIndex();
        processEntries(entries);
    }

    // ---

    private Pattern indexLineRe = Pattern.compile("^([a-z0-9_-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*");

    @NonNull
    private List<HtmlEntry> readIndex() throws IOException {
        mLogger.d(TAG, "Processing document: index");
        String indexId = mFlags.getString(EXP_DOC_ID);
        byte[] bytes = getGDoc(indexId, "text/plain");
        String content = new String(bytes, Charsets.UTF_8);

        List<HtmlEntry> entries = new ArrayList<>();

        for (String line : content.split("\n")) {
            line = line.trim();
            Matcher matcher = indexLineRe.matcher(line);
            if (matcher.find()) {
                entries.add(HtmlEntry.create(matcher.group(0), matcher.group(1)));
            }
        }

        return entries;
    }

    private void processEntries(@NonNull List<HtmlEntry> entries) {
        for (HtmlEntry entry : entries) {
            mLogger.d(TAG, "Process document: " + entry.getDestName());
        }
    }


    // ---

    @Null
    private byte[] getGDoc(@NonNull String fileId, @NonNull String mimeType) {
        final String hashKey = "hash-" + fileId;
        final String contentKey = "content-" + fileId;

        // Known implementation issue: the gdoc API calls to retrieve the file content
        // and the freshness hash are not part of an atomic call. There's a chance the
        // server-side data has changed when retrieving both. However we get the hash
        // first (say v1) and later get the content (say v2). In the store we keep
        // hash(v1) + data(v2). Next time this method is checked, it will check the hash
        // and get hash(v2) from the server. It does not match and thus retrieves again
        // data(v2).
        // If synchronization were important, a way to mitigate this is to get the hash
        // to check the freshness. When getting the data, get the hash again and retry
        // few times if it keeps changing.
        // In the current context of rig with very little server-side changes and a daily
        // check, the current flaw is acceptable enough.

        // Check store data
        byte[] content = null;
        try {
            content = mBlobStore.getBytes(contentKey);
        } catch (IOException ignore) {}

        String contentHash = null;
        try {
            contentHash = mGDocReader.getContentHashById(fileId);
        } catch (IOException ignore) {}

        if (contentHash == null) {
            return null;
        }

        boolean retrieve = true;
        if (content != null) {
            // Check freshness
            try {
                String storeHash = mBlobStore.getString(hashKey);
                retrieve = !contentHash.equals(storeHash);
            } catch (IOException ignore) {}
        }

        if (retrieve) {
            try {
                mLogger.d(TAG, "        Fetching: " + fileId);
                content = mGDocReader.readFileById(fileId, mimeType);

                if (content != null) {
                    // Update the store
                    mBlobStore.putBytes(contentKey, content);
                    mBlobStore.putString(hashKey, contentHash);
                }
            } catch (IOException ignore) {}
        }

        return content;
    }

    @AutoValue
    static abstract class HtmlEntry {
        public static HtmlEntry create(@NonNull String fileId, @NonNull String destName) {
            return new AutoValue_Exp_HtmlEntry(fileId, destName);
        }

        abstract String getFileId();
        abstract String getDestName();
    }
}
