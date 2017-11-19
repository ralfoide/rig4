package com.alflabs.rig4.exp;

import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import com.google.common.base.Charsets;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class Exp {
    private static final String TAG = Exp.class.getSimpleName();

    private static final String EXP_DOC_ID = "exp-doc-id";
    private static final String EXP_DEST_DIR = "exp-dest-dir";
    private static final String EXP_GA_UID = "exp-ga-uid";

    private final Flags mFlags;
    private final ILogger mLogger;
    private final GDocReader mGDocReader;

    @Inject
    public Exp(Flags flags, ILogger logger, GDocReader gDocReader) {
        mFlags = flags;
        mLogger = logger;
        mGDocReader = gDocReader;

        mFlags.addString(EXP_DOC_ID, "", null);
        mFlags.addString(EXP_DEST_DIR, "", null);
        mFlags.addString(EXP_GA_UID, "", null);
    }

    public void start() throws IOException {
        readIndex();
    }

    private void readIndex() throws IOException {
        String indexId = mFlags.getString(EXP_DOC_ID);
        byte[] bytes = mGDocReader.readFileById(indexId, "text/plain");
        String content = new String(bytes, Charsets.UTF_8);
        mLogger.d(TAG, "Index: \n " + content);

        mLogger.d(TAG, "Hash: " + mGDocReader.getMetadataHashById(indexId));
    }
}
